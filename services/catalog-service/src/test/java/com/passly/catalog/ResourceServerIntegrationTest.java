package com.passly.catalog;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ResourceServerIntegrationTest {

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
