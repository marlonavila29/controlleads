package com.controlleads.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * SLA/follow-up sweep behaviour: alerts land in the owner's in-app center,
 * anti-spam dedup prevents repeats within a cycle, and the read endpoints work.
 * The scheduler is disabled (application.properties) so sweeps run only when
 * this test invokes NotificationService directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationSweepTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	MockMvc mvc;

	@Autowired
	NotificationService notificationService;

	final ObjectMapper mapper = new ObjectMapper();

	String adminToken;
	String mariaToken;
	String joaoToken;
	String courseId;
	String channelId;

	@BeforeAll
	void setup() throws Exception {
		adminToken = login("admin@controlleads.local", "admin123");
		createUser("Maria", "maria@notif.local", "maria12345");
		createUser("Joao", "joao@notif.local", "joao12345");
		mariaToken = login("maria@notif.local", "maria12345");
		joaoToken = login("joao@notif.local", "joao12345");
		courseId = apiPost("/api/courses", adminToken, "{\"name\":\"Engineering\"}", 201).get("id").asText();
		channelId = apiGet("/api/channels", adminToken).get(0).get("id").asText();
	}

	@Test
	void slaSweepAlertsOwnerAndDedupes() throws Exception {
		String lead = createLead(mariaToken, "Cold Prospect");
		transition(mariaToken, lead, "HOT_LEAD"); // uncontacted → breaches immediately

		int first = notificationService.runSlaSweep();
		assertThat(first).isGreaterThanOrEqualTo(1);

		// Anti-spam: a second immediate sweep creates nothing new for the same lead.
		int second = notificationService.runSlaSweep();
		assertThat(second).isZero();

		JsonNode list = apiGet("/api/notifications", mariaToken);
		JsonNode alert = null;
		for (JsonNode n : list) {
			if (n.get("type").asText().equals("SLA_BREACH") && n.get("leadId").asText().equals(lead)) {
				alert = n;
			}
		}
		assertThat(alert).isNotNull();
		assertThat(alert.get("leadName").asText()).isEqualTo("Cold Prospect");

		// Joao (not the owner) receives nothing.
		assertThat(apiGet("/api/notifications", joaoToken).size()).isZero();
	}

	@Test
	void followUpSweepAlertsOnDueTask() throws Exception {
		String lead = createLead(mariaToken, "Callback Later");
		String past = Instant.now().minus(1, ChronoUnit.HOURS).toString();
		apiPost("/api/leads/" + lead + "/activities", mariaToken,
			"{\"type\":\"FOLLOW_UP\",\"content\":\"Overdue callback\",\"dueAt\":\"" + past + "\"}", 200);

		int created = notificationService.runFollowUpSweep();
		assertThat(created).isGreaterThanOrEqualTo(1);

		boolean found = false;
		for (JsonNode n : apiGet("/api/notifications", mariaToken)) {
			if (n.get("type").asText().equals("FOLLOW_UP_DUE") && n.get("leadId").asText().equals(lead)) {
				found = true;
			}
		}
		assertThat(found).isTrue();
	}

	@Test
	void unreadCountAndMarkRead() throws Exception {
		String lead = createLead(joaoToken, "Joao Prospect");
		transition(joaoToken, lead, "HOT_LEAD");
		notificationService.runSlaSweep();

		long unreadBefore = apiGet("/api/notifications/unread-count", joaoToken).get("count").asLong();
		assertThat(unreadBefore).isGreaterThanOrEqualTo(1);

		String notifId = null;
		for (JsonNode n : apiGet("/api/notifications", joaoToken)) {
			if (n.get("leadId").asText().equals(lead)) notifId = n.get("id").asText();
		}
		assertThat(notifId).isNotNull();

		mvc.perform(post("/api/notifications/" + notifId + "/read")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + joaoToken))
			.andExpect(status().isNoContent());

		long unreadAfter = apiGet("/api/notifications/unread-count", joaoToken).get("count").asLong();
		assertThat(unreadAfter).isEqualTo(unreadBefore - 1);

		mvc.perform(post("/api/notifications/read-all")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + joaoToken))
			.andExpect(status().isNoContent());
		assertThat(apiGet("/api/notifications/unread-count", joaoToken).get("count").asLong()).isZero();
	}

	@Test
	void leadNameHiddenFromFormerOwnerAfterReassign() throws Exception {
		createUser("Rita", "rita@notif.local", "rita12345");
		createUser("Ravi", "ravi@notif.local", "ravi12345");
		String ritaToken = login("rita@notif.local", "rita12345");
		String raviId = null;
		for (JsonNode u : apiGet("/api/users", adminToken)) {
			if (u.get("email").asText().equals("ravi@notif.local")) raviId = u.get("id").asText();
		}

		String lead = createLead(ritaToken, "Reassign Me");
		transition(ritaToken, lead, "HOT_LEAD");
		notificationService.runSlaSweep();

		// While Rita owns the lead, her notification shows its name.
		assertThat(leadNameOf(apiGet("/api/notifications", ritaToken), lead)).isEqualTo("Reassign Me");

		// Admin reassigns the lead away from Rita.
		apiPatch("/api/leads/" + lead, adminToken, "{\"assignedTo\":\"" + raviId + "\"}");

		// Rita still holds the stale notification, but its name is now hidden.
		JsonNode after = apiGet("/api/notifications", ritaToken);
		JsonNode stale = null;
		for (JsonNode n : after) {
			if (n.has("leadId") && lead.equals(n.get("leadId").asText())) stale = n;
		}
		assertThat(stale).isNotNull();
		assertThat(stale.get("leadName").isNull()).isTrue();
	}

	// ---- helpers -------------------------------------------------------------

	private String leadNameOf(JsonNode notifications, String leadId) {
		for (JsonNode n : notifications) {
			if (n.has("leadId") && leadId.equals(n.get("leadId").asText())) {
				return n.get("leadName").asText();
			}
		}
		return null;
	}

	private void apiPatch(String path, String token, String json) throws Exception {
		mvc.perform(patch(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk());
	}

	private String createLead(String token, String name) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("fullName", name);
		body.put("countryCode", "US");
		body.put("email", name.replace(' ', '.').toLowerCase() + "@x.com");
		body.put("courseId", courseId);
		body.put("channelId", channelId);
		return apiPost("/api/leads", token, mapper.writeValueAsString(body), 201).get("id").asText();
	}

	private void transition(String token, String leadId, String to) throws Exception {
		apiPost("/api/leads/" + leadId + "/transition", token, "{\"toStatus\":\"" + to + "\"}", 200);
	}

	private JsonNode apiGet(String path, String token) throws Exception {
		String body = mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return mapper.readTree(body);
	}

	private JsonNode apiPost(String path, String token, String json, int expected) throws Exception {
		String body = mvc.perform(post(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().is(expected))
			.andReturn().getResponse().getContentAsString();
		return body.isEmpty() ? mapper.createObjectNode() : mapper.readTree(body);
	}

	private String login(String email, String password) throws Exception {
		String body = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return mapper.readTree(body).get("accessToken").asText();
	}

	private void createUser(String name, String email, String password) throws Exception {
		apiPost("/api/users", adminToken, mapper.writeValueAsString(Map.of(
			"name", name, "email", email, "password", password, "role", "MARKETING_TEAM")), 201);
	}
}
