package com.controlleads.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full password-reset flow against a real Postgres. A capturing mailer exposes
 * the raw token (normally only emailed) so the test can complete the reset,
 * and asserts single-use + old-password invalidation + no email enumeration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordResetTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	/**
	 * Overrides the default logging mailer so tests can read the raw token.
	 * Keyed by email (the real send is now async/after-commit), which also keeps
	 * concurrent tests from stepping on each other.
	 */
	static final class CapturingMailer implements PasswordResetMailer {
		final Map<String, String> tokensByEmail = new ConcurrentHashMap<>();

		@Override
		public void sendResetLink(String email, String rawToken) {
			tokensByEmail.put(email, rawToken);
		}
	}

	@TestConfiguration
	static class Config {
		@Bean
		@Primary
		CapturingMailer capturingMailer() {
			return new CapturingMailer();
		}
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	CapturingMailer mailer;

	final ObjectMapper mapper = new ObjectMapper();

	String adminToken;

	@BeforeAll
	void setup() throws Exception {
		adminToken = login("admin@controlleads.local", "admin123");
		createUser("Reset Me", "reset@pw.local", "original123");
	}

	@Test
	void fullResetFlow() throws Exception {
		forgot("reset@pw.local");

		String token = awaitToken("reset@pw.local"); // email is sent async after commit
		assertThat(token).isNotBlank();

		// Reset succeeds; the old password stops working and the new one works.
		reset(token, "brandnew123", 204);
		loginExpect("reset@pw.local", "original123", 401);
		loginExpect("reset@pw.local", "brandnew123", 200);

		// The token is single-use.
		reset(token, "another123", 400);
	}

	@Test
	void unknownEmailIsSilent() throws Exception {
		forgot("nobody@pw.local"); // still 204, no enumeration
		Thread.sleep(300); // give any (non-existent) async send a chance to fire
		assertThat(mailer.tokensByEmail.get("nobody@pw.local")).isNull();
	}

	@Test
	void invalidTokenRejected() throws Exception {
		reset("not-a-real-token", "whatever123", 400);
	}

	// ---- helpers -------------------------------------------------------------

	/** Waits for the after-commit async mailer to capture the token for an email. */
	private String awaitToken(String email) throws InterruptedException {
		for (int i = 0; i < 60; i++) {
			String token = mailer.tokensByEmail.get(email);
			if (token != null) {
				return token;
			}
			Thread.sleep(50);
		}
		return null;
	}

	private void forgot(String email) throws Exception {
		mvc.perform(post("/api/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("email", email))))
			.andExpect(status().isNoContent());
	}

	private void reset(String token, String newPassword, int expected) throws Exception {
		mvc.perform(post("/api/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("token", token, "newPassword", newPassword))))
			.andExpect(status().is(expected));
	}

	private void loginExpect(String email, String password, int expected) throws Exception {
		mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
			.andExpect(status().is(expected));
	}

	private String login(String email, String password) throws Exception {
		String body = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return mapper.readTree(body).get("accessToken").asText();
	}

	private void createUser(String name, String email, String password) throws Exception {
		mvc.perform(post("/api/users")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(Map.of(
					"name", name, "email", email, "password", password, "role", "MARKETING_TEAM"))))
			.andExpect(status().isCreated());
	}
}
