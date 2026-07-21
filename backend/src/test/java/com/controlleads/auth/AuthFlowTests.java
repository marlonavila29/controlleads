package com.controlleads.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
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
 * End-to-end auth flow against a real Postgres:
 * login, role enforcement, refresh rotation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	MockMvc mvc;

	final ObjectMapper mapper = new ObjectMapper();

	private JsonNode loginAdmin() throws Exception {
		String body = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(
					Map.of("email", "admin@controlleads.local", "password", "admin123"))))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return mapper.readTree(body);
	}

	@Test
	@Order(1)
	void adminCanLogin() throws Exception {
		JsonNode auth = loginAdmin();
		assertThat(auth.get("accessToken").asText()).isNotBlank();
		assertThat(auth.get("refreshToken").asText()).isNotBlank();
		assertThat(auth.at("/user/role").asText()).isEqualTo("ADMINISTRATOR");
	}

	@Test
	@Order(2)
	void wrongPasswordIsRejected() throws Exception {
		mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(
					Map.of("email", "admin@controlleads.local", "password", "nope"))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@Order(3)
	void anonymousCannotListUsers() throws Exception {
		mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
	}

	@Test
	@Order(4)
	void marketingMemberCannotManageUsers() throws Exception {
		JsonNode admin = loginAdmin();
		mvc.perform(post("/api/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.get("accessToken").asText())
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of(
					"name", "Maria Recruiter",
					"email", "maria@controlleads.local",
					"password", "maria12345",
					"role", "MARKETING_TEAM"))))
			.andExpect(status().isCreated());

		String mariaBody = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(
					Map.of("email", "maria@controlleads.local", "password", "maria12345"))))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		String mariaToken = mapper.readTree(mariaBody).get("accessToken").asText();

		// Marketing can see own profile...
		mvc.perform(get("/api/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken))
			.andExpect(status().isOk());

		// ...but not the admin-only user list.
		mvc.perform(get("/api/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + mariaToken))
			.andExpect(status().isForbidden());
	}

	@Test
	@Order(5)
	void refreshRotates() throws Exception {
		JsonNode first = loginAdmin();
		String firstRefresh = first.get("refreshToken").asText();

		String refreshedBody = mvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("refreshToken", firstRefresh))))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		assertThat(mapper.readTree(refreshedBody).get("refreshToken").asText())
			.isNotEqualTo(firstRefresh);

		// The used refresh token is revoked — replay must fail.
		mvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("refreshToken", firstRefresh))))
			.andExpect(status().isUnauthorized());
	}
}
