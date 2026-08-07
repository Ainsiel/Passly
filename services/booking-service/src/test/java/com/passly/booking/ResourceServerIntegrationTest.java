package com.passly.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passly.booking.adapter.out.persistence.EventProjectionJpaRepository;
import com.passly.booking.support.AbstractMessagingIntegrationTest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceServerIntegrationTest extends AbstractMessagingIntegrationTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Container
	static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.7")
		.withRealmImportFile("/passly-realm/realm-export.json");

	@DynamicPropertySource
	static void keycloakProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
			() -> KEYCLOAK.getAuthServerUrl() + "/realms/passly");
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	EventProjectionJpaRepository eventProjectionRepository;

	@Autowired
	org.springframework.transaction.support.TransactionTemplate transactionTemplate;

	@Autowired
	org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanAndSeedEvent() {
		jdbcTemplate.execute("TRUNCATE TABLE reservations CASCADE");
		transactionTemplate.executeWithoutResult(status -> eventProjectionRepository.upsert(7L, "Noche de Jazz",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), 500, 0));
	}

	@Test
	void unauthenticatedRequestToProtectedEndpointIsRejectedWith401() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":7,\"quantity\":1,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void anAuthenticatedUserCanBookWithARealKeycloakToken() throws Exception {
		String token = obtainAccessToken("user", "user123");

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer " + token)
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":7,\"quantity\":2,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.eventId").value(7))
			.andExpect(jsonPath("$.tickets", org.hamcrest.Matchers.hasSize(2)));
	}

	@Test
	void bookingWithARepeatedIdempotencyKeyIsNotDuplicated() throws Exception {
		String token = obtainAccessToken("user", "user123");

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer " + token)
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":7,\"quantity\":1,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer " + token)
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":7,\"quantity\":1,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isOk());

		mockMvc.perform(get("/reservas").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
	}

	private static String obtainAccessToken(String username, String password) throws Exception {
		String tokenEndpoint = KEYCLOAK.getAuthServerUrl() + "/realms/passly/protocol/openid-connect/token";
		HttpRequest request = HttpRequest.newBuilder(URI.create(tokenEndpoint))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(
				"grant_type=password&client_id=admin-cli&client_secret=admin-cli-secret&username=" + username + "&password=" + password))
			.build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IllegalStateException("No se pudo obtener el token: HTTP " + response.statusCode() + " " + response.body());
		}
		JsonNode body = OBJECT_MAPPER.readTree(response.body());
		return body.get("access_token").asText();
	}
}
