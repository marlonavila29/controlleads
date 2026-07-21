package com.controlleads.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
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
 * Full lead lifecycle: creation with events, ownership visibility,
 * funnel validation, STALLED round-trip, terminal STUDENT, duplicates.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LeadFlowTests {

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
	String leadId;

	@BeforeAll
	void setup() throws Exception {
		adminToken = login("admin@controlleads.local", "admin123");
		createUser("Maria Recruiter", "maria@leads.local", "maria12345");
		createUser("Joao Recruiter", "joao@leads.local", "joao12345");
		mariaToken = login("maria@leads.local", "maria12345");
		joaoToken = login("joao@leads.local", "joao12345");

		courseId = mapper.readTree(mvc.perform(post("/api/courses")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Nursing BSc\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString()).get("id").asText();

		JsonNode channels = mapper.readTree(mvc.perform(get("/api/channels")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString());
		channelId = channels.get(0).get("id").asText();

		JsonNode reasons = mapper.readTree(mvc.perform(get("/api/stall-reasons")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString());
		stallReasonId = reasons.get(0).get("id").asText();
	}

	@Test
	@Order(1)
	void memberCreatesLeadAndOwnsIt() throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("fullName", "Nguyen Van An");
		body.put("countryCode", "vn");
		body.put("email", "an@example.com");
		body.put("phone", "+84 912 345 678");
		body.put("courseId", courseId);
		body.put("channelId", channelId);
		body.put("utmSource", "instagram");

		JsonNode lead = mapper.readTree(mvc.perform(post("/api/leads")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString());

		leadId = lead.get("id").asText();
		assertThat(lead.get("status").asText()).isEqualTo("LEAD");
		assertThat(lead.get("countryCode").asText()).isEqualTo("VN");
		assertThat(lead.get("assignedToName").asText()).isEqualTo("Maria Recruiter");
	}

	@Test
	@Order(2)
	void ownershipIsEnforced() throws Exception {
		// João cannot open Maria's lead (404, not 403 — no id probing).
		mvc.perform(get("/api/leads/" + leadId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + joaoToken))
			.andExpect(status().isNotFound());

		// João's list is empty; admin sees the lead.
		JsonNode joaoList = mapper.readTree(mvc.perform(get("/api/leads")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + joaoToken))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		assertThat(joaoList.get("totalElements").asLong()).isZero();

		JsonNode adminList = mapper.readTree(mvc.perform(get("/api/leads")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		assertThat(adminList.get("totalElements").asLong()).isEqualTo(1);
	}

	@Test
	@Order(3)
	void funnelRejectsSkippingStages() throws Exception {
		mvc.perform(post("/api/leads/" + leadId + "/transition")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"toStatus\":\"STUDENT\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@Order(4)
	void stalledRoundTrip() throws Exception {
		transition(mariaToken, "HOT_LEAD", null, 200);
		transition(mariaToken, "APPLICATION", null, 200);

		// Stalling requires a reason...
		transition(mariaToken, "STALLED", null, 400);
		// ...and records the stage it left.
		JsonNode stalled = transition(mariaToken, "STALLED", stallReasonId, 200);
		assertThat(stalled.get("stalledFromStatus").asText()).isEqualTo("APPLICATION");

		// Reactivation only back to that stage.
		transition(mariaToken, "HOT_LEAD", null, 400);
		JsonNode reactivated = transition(mariaToken, "APPLICATION", null, 200);
		assertThat(reactivated.get("stalledFromStatus").isNull()).isTrue();
	}

	@Test
	@Order(5)
	void studentIsTerminalForMembers() throws Exception {
		transition(mariaToken, "STUDENT", null, 200);
		transition(mariaToken, "APPLICATION", null, 400);
	}

	@Test
	@Order(6)
	void statusHistoryIsComplete() throws Exception {
		JsonNode detail = mapper.readTree(mvc.perform(get("/api/leads/" + leadId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

		JsonNode history = detail.get("statusHistory");
		// creation + HOT_LEAD + APPLICATION + STALLED + APPLICATION + STUDENT
		assertThat(history.size()).isEqualTo(6);
		assertThat(history.get(0).get("fromStatus").isNull()).isTrue();
		assertThat(history.get(0).get("toStatus").asText()).isEqualTo("LEAD");
		assertThat(history.get(5).get("toStatus").asText()).isEqualTo("STUDENT");
		assertThat(history.get(3).get("stallReasonId").asText()).isEqualTo(stallReasonId);
	}

	@Test
	@Order(7)
	void duplicatesWarnButDontBlock() throws Exception {
		JsonNode duplicates = mapper.readTree(mvc.perform(get("/api/leads/duplicates")
				.param("email", "an@example.com")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		assertThat(duplicates.size()).isEqualTo(1);
		assertThat(duplicates.get(0).get("fullName").asText()).isEqualTo("Nguyen Van An");
	}

	// ----- helpers -----

	private JsonNode transition(String token, String to, String reasonId, int expected) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("toStatus", to);
		if (reasonId != null) body.put("stallReasonId", reasonId);
		String response = mvc.perform(post("/api/leads/" + leadId + "/transition")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(body)))
			.andExpect(status().is(expected))
			.andReturn().getResponse().getContentAsString();
		return response.isEmpty() ? null : mapper.readTree(response);
	}

	private String login(String email, String password) throws Exception {
		String body = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return mapper.readTree(body).get("accessToken").asText();
	}

	private void createUser(String name, String email, String password) throws Exception {
		mvc.perform(post("/api/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of(
					"name", name, "email", email, "password", password, "role", "MARKETING_TEAM"))))
			.andExpect(status().isCreated());
	}
}
