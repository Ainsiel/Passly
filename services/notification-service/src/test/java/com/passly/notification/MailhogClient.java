package com.passly.notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente HTTP del API REST de Mailhog (puerto 8025) para los tests de
 * integración: consulta los mensajes entregados por SMTP y vacía el buzón.
 */
final class MailhogClient {

	private final String apiBaseUrl;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	MailhogClient(String apiBaseUrl) {
		this.apiBaseUrl = apiBaseUrl;
	}

	List<Email> messages() {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiBaseUrl + "/api/v2/messages")).GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode root = jsonMapper.readTree(response.body());
			List<Email> emails = new ArrayList<>();
			for (JsonNode item : root.path("items")) {
				JsonNode content = item.path("Content");
				String subject = firstHeader(content, "Subject");
				String to = firstHeader(content, "To");
				String html = content.path("Body").asText();
				emails.add(new Email(subject, to, html));
			}
			return emails;
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo consultar el API de Mailhog", e);
		}
	}

	void purge() {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiBaseUrl + "/api/v1/messages")).DELETE()
					.build();
			client.send(request, HttpResponse.BodyHandlers.discarding());
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo vaciar el buzón de Mailhog", e);
		}
	}

	Email awaitEmail(String recipient) {
		long deadline = System.currentTimeMillis() + 30_000;
		while (System.currentTimeMillis() < deadline) {
			for (Email email : messages()) {
				if (email.to() != null && email.to().contains(recipient)) {
					return email;
				}
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
		}
		throw new AssertionError("No se recibió ningún email para " + recipient + " en 30s");
	}

	private String firstHeader(JsonNode content, String name) {
		JsonNode values = content.path("Headers").path(name);
		if (values.isArray() && !values.isEmpty()) {
			return values.get(0).asText();
		}
		return null;
	}

	record Email(String subject, String to, String htmlBody) {
	}
}
