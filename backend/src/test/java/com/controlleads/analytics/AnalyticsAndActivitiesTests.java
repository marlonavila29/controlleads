package com.controlleads.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Exercises Activities (1c) and Analytics (1d) end to end against a real
 * Postgres, verifying that the event-derived numbers match a driven lifecycle.
 *
 * Fixture (owner → lead → path):
 *   Maria → A (VN) LEAD→HOT→APPLICATION→STUDENT     (+ CALL, + FOLLOW_UP)
 *   Maria → B (BR) LEAD→HOT→STALLED(reason)
 *   Maria → D (VN) LEAD→HOT                          (uncontacted HOT → SLA breach)
 *   Joao  → C (IN) LEAD
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsAndActivitiesTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	MockMvc mvc;

	final ObjectMapper mapper = new ObjectMapper();

	String adminToken;
	String mariaToken;
	String joaoToken;
	String courseId;
	String channelId;
	String stallReasonId;
	String leadA;
	String leadB;

	@BeforeAll
	void setup() throws Exception {
		adminToken = login("admin@controlleads.local", "admin123");
		createUser("Maria Recruiter", "maria@leads.local", "maria12345");
		createUser("Joao Recruiter", "joao@leads.local", "joao12345");
		mariaToken = login("maria@leads.local", "maria12345");
		joaoToken = login("joao@leads.local", "joao12345");

		courseId = apiPost("/api/courses", adminToken, "{\"name\":\"Nursing BSc\"}", 201).get("id").asText();
		channelId = apiGet("/api/channels", adminToken).get(0).get("id").asText();
		stallReasonId = apiGet("/api/stall-reasons", adminToken).get(0).get("id").asText();

		leadA = createLead(mariaToken, "Nguyen Van An", "VN", "an@x.com");
		leadB = createLead(mariaToken, "Bruno Souza", "BR", "bruno@x.com");
		String leadD = createLead(mariaToken, "Duc Pham", "VN", "duc@x.com");
		createLead(joaoToken, "Isha Rao", "IN", "isha@x.com"); // C

		transition(mariaToken, leadA, "HOT_LEAD", null);
		transition(mariaToken, leadA, "APPLICATION", null);
		transition(mariaToken, leadA, "STUDENT", null);

		transition(mariaToken, leadB, "HOT_LEAD", null);
		transition(mariaToken, leadB, "STALLED", stallReasonId);

		transition(mariaToken, leadD, "HOT_LEAD", null); // uncontacted HOT
	}

	// ---- Activities (1c) -----------------------------------------------------

	@Test
	void contactActivityResetsSlaClock() throws Exception {
		apiPost("/api/leads/" + leadA + "/activities", mariaToken,
			"{\"type\":\"CALL\",\"content\":\"Called about documents\"}", 200);

		JsonNode detail = apiGet("/api/leads/" + leadA, mariaToken);
		assertThat(detail.at("/lead/lastContactedAt").isNull()).isFalse();
	}

	@Test
	void followUpAppearsInMyTasksAndCompletes() throws Exception {
		String due = Instant.now().plus(3, ChronoUnit.DAYS).toString();
		String activityId = apiPost("/api/leads/" + leadA + "/activities", mariaToken,
			"{\"type\":\"FOLLOW_UP\",\"content\":\"Call back\",\"dueAt\":\"" + due + "\"}", 200)
			.get("id").asText();

		JsonNode myTasks = apiGet("/api/my/follow-ups", mariaToken);
		assertThat(myTasks.size()).isGreaterThanOrEqualTo(1);
		assertThat(myTasks.get(0).get("leadName").asText()).isEqualTo("Nguyen Van An");

		// Joao has none of Maria's follow-ups.
		assertThat(apiGet("/api/my/follow-ups", joaoToken).size()).isZero();

		apiPost("/api/activities/" + activityId + "/complete", mariaToken, "{}", 200);
		boolean stillOpen = false;
		for (JsonNode t : apiGet("/api/my/follow-ups", mariaToken)) {
			if (t.get("id").asText().equals(activityId)) stillOpen = true;
		}
		assertThat(stillOpen).isFalse();
	}

	@Test
	void activitiesRespectOwnership() throws Exception {
		mvc.perform(get("/api/leads/" + leadA + "/activities")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + joaoToken))
			.andExpect(status().isNotFound());
	}

	// ---- Analytics (1d) ------------------------------------------------------

	@Test
	void summaryReflectsLifecycleForMember() throws Exception {
		JsonNode s = apiGet("/api/analytics/summary", mariaToken);
		assertThat(s.get("totalLeads").asLong()).isEqualTo(3);   // A, B, D
		assertThat(s.get("conversionRate").asDouble()).isBetween(0.32, 0.34); // 1/3
		assertThat(s.get("slaBreaches").asLong()).isEqualTo(1);  // D: HOT, uncontacted
		assertThat(s.get("avgDaysToConvert").isNull()).isFalse();
	}

	@Test
	void adminSummarySeesEveryone() throws Exception {
		JsonNode s = apiGet("/api/analytics/summary", adminToken);
		assertThat(s.get("totalLeads").asLong()).isEqualTo(4);   // A, B, C, D
	}

	@Test
	void funnelIsEventDerived() throws Exception {
		Map<String, Long> counts = new HashMap<>();
		for (JsonNode stage : apiGet("/api/analytics/funnel", mariaToken)) {
			counts.put(stage.get("status").asText(), stage.get("count").asLong());
		}
		assertThat(counts.get("LEAD")).isEqualTo(3);
		assertThat(counts.get("HOT_LEAD")).isEqualTo(3);
		assertThat(counts.get("APPLICATION")).isEqualTo(1);
		assertThat(counts.get("STUDENT")).isEqualTo(1);
	}

	@Test
	void dropOffShowsStageAndReason() throws Exception {
		JsonNode rows = apiGet("/api/analytics/drop-off", mariaToken);
		assertThat(rows.size()).isEqualTo(1);
		assertThat(rows.get(0).get("stage").asText()).isEqualTo("HOT_LEAD");
		assertThat(rows.get(0).get("reasonId").asText()).isEqualTo(stallReasonId);
		assertThat(rows.get(0).get("count").asLong()).isEqualTo(1);
	}

	@Test
	void breakdownsAndCountry() throws Exception {
		JsonNode channels = apiGet("/api/analytics/by-channel", mariaToken);
		assertThat(channels.get(0).get("total").asLong()).isEqualTo(3);
		assertThat(channels.get(0).get("students").asLong()).isEqualTo(1);

		long vnTotal = 0;
		long vnStudents = 0;
		for (JsonNode c : apiGet("/api/analytics/by-country", mariaToken)) {
			if (c.get("countryCode").asText().equals("VN")) {
				vnTotal = c.get("total").asLong();
				vnStudents = c.get("students").asLong();
			}
		}
		assertThat(vnTotal).isEqualTo(2);   // A + D
		assertThat(vnStudents).isEqualTo(1); // A
	}

	@Test
	void leaderboardIsAdminOnly() throws Exception {
		mvc.perform(get("/api/analytics/leaderboard")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken))
			.andExpect(status().isForbidden());

		JsonNode board = apiGet("/api/analytics/leaderboard", adminToken);
		boolean mariaSeen = false;
		for (JsonNode row : board) {
			if (row.get("name").asText().equals("Maria Recruiter")) {
				mariaSeen = true;
				assertThat(row.get("students").asLong()).isEqualTo(1);
				assertThat(row.get("totalLeads").asLong()).isEqualTo(3);
			}
		}
		assertThat(mariaSeen).isTrue();
	}

	@Test
	void timeseriesCountsConversions() throws Exception {
		long converted = 0;
		for (JsonNode point : apiGet("/api/analytics/timeseries", mariaToken)) {
			converted += point.get("converted").asLong();
		}
		assertThat(converted).isEqualTo(1);
	}

	// ---- helpers -------------------------------------------------------------

	private String createLead(String token, String name, String country, String email) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("fullName", name);
		body.put("countryCode", country);
		body.put("email", email);
		body.put("courseId", courseId);
		body.put("channelId", channelId);
		return apiPost("/api/leads", token, mapper.writeValueAsString(body), 201).get("id").asText();
	}

	private void transition(String token, String leadId, String to, String reasonId) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("toStatus", to);
		if (reasonId != null) body.put("stallReasonId", reasonId);
		apiPost("/api/leads/" + leadId + "/transition", token, mapper.writeValueAsString(body), 200);
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
