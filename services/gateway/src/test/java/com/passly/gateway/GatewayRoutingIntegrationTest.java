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
	private static volatile String receivedBody;

	@LocalServerPort
	int port;

	@BeforeAll
	static void startStubs() throws IOException {
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
		stub.createContext("/reservas", exchange -> {
			receivedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
			receivedPath = exchange.getRequestURI().getPath();
			receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] body = "{\"id\":\"reserva-1\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(201, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		stub.start();
	}

	@AfterAll
	static void stopStubs() {
		stub.stop(0);
	}

	@DynamicPropertySource
	static void serviceUris(DynamicPropertyRegistry registry) {
		registry.add("PASSLY_CATALOG_URI", () -> "http://localhost:" + stub.getAddress().getPort());
		registry.add("PASSLY_BOOKING_URI", () -> "http://localhost:" + stub.getAddress().getPort());
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

	@Test
	void forwardsApiBookingRequestsStrippingPrefixAndPropagatingAuthorization() {
		ResponseEntity<String> response = RestClient.create()
			.post()
			.uri("http://localhost:" + port + "/api/booking/reservas")
			.header("Authorization", "Bearer test-token")
			.header("X-Idempotency-Key", "key-1")
			.body("{\"eventId\":7,\"quantity\":2}")
			.retrieve()
			.toEntity(String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(201);
		assertThat(response.getBody()).contains("\"id\":\"reserva-1\"");
		assertThat(receivedPath).isEqualTo("/reservas");
		assertThat(receivedAuthorization).isEqualTo("Bearer test-token");
		assertThat(receivedBody).contains("\"eventId\":7");
	}
}
