package com.passly.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIntegrationTest {

	private static HttpServer stub;
	private static volatile String receivedAuthorization;
	private static volatile String receivedPath;

	@LocalServerPort
	int port;

	@BeforeAll
	static void startCatalogStub() throws IOException {
		stub = HttpServer.create(new InetSocketAddress(0), 0);
		stub.createContext("/me", exchange -> {
			receivedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
			receivedPath = exchange.getRequestURI().getPath();
			byte[] body = "{\"username\":\"admin\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		stub.start();
	}

	@AfterAll
	static void stopCatalogStub() {
		stub.stop(0);
	}

	@DynamicPropertySource
	static void catalogUri(DynamicPropertyRegistry registry) {
		registry.add("PASSLY_CATALOG_URI", () -> "http://localhost:" + stub.getAddress().getPort());
	}

	@Test
	void forwardsApiCatalogRequestsStrippingPrefixAndPropagatingAuthorization() {
		ResponseEntity<String> response = RestClient.create()
			.get()
			.uri("http://localhost:" + port + "/api/catalog/me")
			.header("Authorization", "Bearer test-token")
			.retrieve()
			.toEntity(String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).contains("\"username\":\"admin\"");
		assertThat(receivedPath).isEqualTo("/me");
		assertThat(receivedAuthorization).isEqualTo("Bearer test-token");
	}
}
