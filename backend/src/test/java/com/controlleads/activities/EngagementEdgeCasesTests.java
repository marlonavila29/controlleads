package com.controlleads.activities;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Regression coverage for the four defects the review workflow confirmed:
 * follow-up completion guard, mandatory due date, distinct-lead drop-off,
 * and follow-ups following lead ownership across reassignment. Isolated DB so
 * the extra leads created here do not perturb the count-based analytics tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EngagementEdgeCasesTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	MockMvc mvc;

	final ObjectMapper mapper = new ObjectMapper();

	String adminToken;
	String mariaToken;
	String joaoToken;
	String carolToken;
	String joaoId;
	String courseId;
	String channelId;
	String stallReasonId;

	@BeforeAll
	void setup() throws Exception {
		adminToken = login("admin@controlleads.local", "admin123");
		createUser("Maria", "maria@edge.local", "maria12345");
		createUser("Joao", "joao@edge.local", "joao12345");
		createUser("Carol", "carol@edge.local", "carol12345");
		mariaToken = login("maria@edge.local", "maria12345");
		joaoToken = login("joao@edge.local", "joao12345");
		carolToken = login("carol@edge.local", "carol12345");

		for (JsonNode u : apiGet("/api/users", adminToken)) {
			if (u.get("email").asText().equals("joao@edge.local")) joaoId = u.get("id").asText();
		}

		courseId = apiPost("/api/courses", adminToken, "{\"name\":\"Data Science\"}", 201).get("id").asText();
		channelId = apiGet("/api/channels", adminToken).get(0).get("id").asText();
		stallReasonId = apiGet("/api/stall-reasons", adminToken).get(0).get("id").asText();
	}

	@Test
	void completeRejectsNonFollowUp() throws Exception {
		String lead = createLead(mariaToken, "Ana", "PT");
		String callId = apiPost("/api/leads/" + lead + "/activities", mariaToken,
			"{\"type\":\"CALL\",\"content\":\"Rang once\"}", 200).get("id").asText();

		mvc.perform(post("/api/activities/" + callId + "/complete")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void followUpRequiresDueDate() throws Exception {
		String lead = createLead(mariaToken, "Beatriz", "PT");

		mvc.perform(post("/api/leads/" + lead + "/activities")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"FOLLOW_UP\",\"content\":\"Call back\"}"))
			.andExpect(status().isBadRequest());

		String due = Instant.now().plus(2, ChronoUnit.DAYS).toString();
		apiPost("/api/leads/" + lead + "/activities", mariaToken,
			"{\"type\":\"FOLLOW_UP\",\"content\":\"Call back\",\"dueAt\":\"" + due + "\"}", 200);
	}

	@Test
	void dropOffCountsDistinctLeadsOnReStall() throws Exception {
		String lead = createLead(carolToken, "Chen", "CN");
		transition(carolToken, lead, "HOT_LEAD", null);
		transition(carolToken, lead, "APPLICATION", null);
		transition(carolToken, lead, "STALLED", stallReasonId);
		transition(carolToken, lead, "APPLICATION", null);   // revive
		transition(carolToken, lead, "STALLED", stallReasonId); // stall again, same stage/reason

		JsonNode rows = apiGet("/api/analytics/drop-off", carolToken);
		assertThat(rows.size()).isEqualTo(1);
		assertThat(rows.get(0).get("stage").asText()).isEqualTo("APPLICATION");
		assertThat(rows.get(0).get("count").asLong()).isEqualTo(1); // distinct lead, not 2 events
	}

	@Test
	void followUpsFollowLeadOwnershipOnReassign() throws Exception {
		String lead = createLead(mariaToken, "Diego", "AR");
		String due = Instant.now().plus(1, ChronoUnit.DAYS).toString();
		String followId = apiPost("/api/leads/" + lead + "/activities", mariaToken,
			"{\"type\":\"FOLLOW_UP\",\"content\":\"Send offer\",\"dueAt\":\"" + due + "\"}", 200)
			.get("id").asText();

		assertThat(containsActivity(apiGet("/api/my/follow-ups", mariaToken), followId)).isTrue();

		// Admin reassigns the lead to Joao.
		apiPatch("/api/leads/" + lead, adminToken, "{\"assignedTo\":\"" + joaoId + "\"}");

		assertThat(containsActivity(apiGet("/api/my/follow-ups", joaoToken), followId)).isTrue();
		assertThat(containsActivity(apiGet("/api/my/follow-ups", mariaToken), followId)).isFalse();

		// New owner can complete it.
		apiPost("/api/activities/" + followId + "/complete", joaoToken, "{}", 200);
	}

	@Test
	void reassignAllMovesLeadsAndUpdatesCounts() throws Exception {
		createUser("Src Owner", "src@edge.local", "src12345678");
		createUser("Dst Owner", "dst@edge.local", "dst12345678");
		String srcToken = login("src@edge.local", "src12345678");
		String dstToken = login("dst@edge.local", "dst12345678");
		String srcId = userId("src@edge.local");
		String dstId = userId("dst@edge.local");

		createLead(srcToken, "Lead One", "US");
		createLead(srcToken, "Lead Two", "US");

		assertThat(leadCount(srcId)).isEqualTo(2);

		JsonNode result = apiPost("/api/users/" + srcId + "/reassign-leads", adminToken,
			"{\"toUserId\":\"" + dstId + "\"}", 200);
		assertThat(result.get("reassigned").asInt()).isEqualTo(2);

		assertThat(leadCount(srcId)).isZero();
		assertThat(leadCount(dstId)).isEqualTo(2);

		// The new owner can now see both leads; the old owner sees none.
		assertThat(apiGet("/api/leads", dstToken).get("totalElements").asLong()).isEqualTo(2);
		assertThat(apiGet("/api/leads", srcToken).get("totalElements").asLong()).isZero();
	}

	@Test
	void csvExportNeutralizesFormulaInjection() throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("fullName", "Formula Guy");
		body.put("countryCode", "US");
		body.put("email", "formula@x.com");
		body.put("courseId", courseId);
		body.put("channelId", channelId);
		body.put("utmSource", "=HYPERLINK(\"http://evil\")");
		apiPost("/api/leads", mariaToken, mapper.writeValueAsString(body), 201);

		String csv = mvc.perform(get("/api/leads/export.csv")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		assertThat(csv).contains("'=HYPERLINK");        // neutralized: text, not a formula
		assertThat(csv).doesNotContain("\"=HYPERLINK");  // no cell opening straight into a formula
	}

	// ---- helpers -------------------------------------------------------------

	private long leadCount(String userId) throws Exception {
		for (JsonNode u : apiGet("/api/users", adminToken)) {
			if (u.get("id").asText().equals(userId)) return u.get("leadCount").asLong();
		}
		return -1;
	}

	private String userId(String email) throws Exception {
		for (JsonNode u : apiGet("/api/users", adminToken)) {
			if (u.get("email").asText().equals(email)) return u.get("id").asText();
		}
		return null;
	}

	private boolean containsActivity(JsonNode list, String activityId) {
		for (JsonNode a : list) {
			if (a.get("id").asText().equals(activityId)) return true;
		}
		return false;
	}

	private String createLead(String token, String name, String country) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("fullName", name);
		body.put("countryCode", country);
		body.put("email", name.toLowerCase() + "@x.com");
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

	private void apiPatch(String path, String token, String json) throws Exception {
		MockHttpServletRequestBuilder req = patch(path)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON).content(json);
		mvc.perform(req).andExpect(status().isOk());
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
