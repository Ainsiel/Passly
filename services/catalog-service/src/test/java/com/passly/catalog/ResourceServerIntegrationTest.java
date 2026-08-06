package com.passly.catalog;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passly.catalog.support.AbstractPostgresIntegrationTest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
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
class ResourceServerIntegrationTest extends AbstractPostgresIntegrationTest {

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

	@Test
	void unauthenticatedRequestToProtectedEndpointIsRejectedWith401() throws Exception {
		mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void authenticatedRequestReturnsUsernameAndRealmRoles() throws Exception {
		String token = obtainAccessToken("admin", "admin123");

		mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"))
			.andExpect(jsonPath("$.roles").value(containsInAnyOrder("ADMIN", "USER")));
	}

	@Test
	void adminCanUpdateAnEventButAUserWithoutAdminRoleIsForbidden() throws Exception {
		String adminToken = obtainAccessToken("admin", "admin123");
		String userToken = obtainAccessToken("user", "user123");

		String editedBody = """
			{"name":"Concierto de la Orquesta Sinfónica de Madrid",
			"description":"Noche editada por el admin.","category":"CONCIERTO",
			"venue":"Auditorio Nacional","startsAt":"2026-09-12T20:00:00",
			"price":45.00,"capacity":2200}
			""";

		mockMvc.perform(put("/events/1")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(editedBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.description").value("Noche editada por el admin."));

		String originalBody = """
			{"name":"Concierto de la Orquesta Sinfónica de Madrid",
			"description":"Noche de obras maestras sinfónicas dirigidas por la titular.","category":"CONCIERTO",
			"venue":"Auditorio Nacional","startsAt":"2026-09-12T20:00:00",
			"price":45.00,"capacity":2200}
			""";
		mockMvc.perform(put("/events/1")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(originalBody))
			.andExpect(status().isOk());

		mockMvc.perform(put("/events/1")
				.header("Authorization", "Bearer " + userToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(editedBody))
			.andExpect(status().isForbidden());

		mockMvc.perform(delete("/events/1").header("Authorization", "Bearer " + userToken))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanCreateAndDeleteAnEventButUserCannotCreate() throws Exception {
		String adminToken = obtainAccessToken("admin", "admin123");
		String userToken = obtainAccessToken("user", "user123");

		String body = """
			{"name":"Evento real del admin","description":null,"category":"CONCIERTO","venue":"Sala",
			"startsAt":"2026-12-31T21:00:00","price":20.00,"capacity":100}
			""";

		String location = mockMvc.perform(post("/events")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Evento real del admin"))
			.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(delete(location).header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/events")
				.header("Authorization", "Bearer " + userToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isForbidden());
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
