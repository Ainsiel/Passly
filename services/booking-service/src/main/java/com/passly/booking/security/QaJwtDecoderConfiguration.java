package com.passly.booking.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Decoder JWT para el perfil {@code qa}. Acepta cualquier token Bearer y extrae
 * los claims ({@code sub}, {@code preferred_username}, {@code realm_access}) del
 * payload JWT sin validar firma. Diseñado para load testing con k6 y E2E con
 * Playwright: no requiere Keycloak ni validación de firma.
 * <p>
 * El {@code sub} se usa como {@code userId} en el dominio Reservas, por lo que
 * debe ser un identificador corto (p.ej. el UUID de Keycloak), no el token
 * completo, para respetar la longitud de la columna {@code user_id VARCHAR(100)}.
 */
@Configuration
@Profile("qa")
public class QaJwtDecoderConfiguration {

	private static final Logger log = LoggerFactory.getLogger(QaJwtDecoderConfiguration.class);

	@Bean
	@Primary
	JwtDecoder qaJwtDecoder() {
		ObjectMapper mapper = new ObjectMapper();
		return token -> {
			try {
				String[] parts = token.split("\\.");
				if (parts.length >= 2) {
					byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
					String payload = new String(payloadBytes, StandardCharsets.UTF_8);
					JsonNode claims = mapper.readTree(payload);

					String sub = claims.has("sub") ? claims.get("sub").asText() : token;
					String username = claims.has("preferred_username")
						? claims.get("preferred_username").asText() : sub;

					List<String> roles = new ArrayList<>();
					if (claims.has("realm_access") && claims.get("realm_access").has("roles")) {
						claims.get("realm_access").get("roles").forEach(node -> roles.add(node.asText()));
					}
					if (roles.isEmpty()) {
						roles.add("USER");
					}

					return Jwt.withTokenValue(token)
						.header("alg", "none")
						.subject(sub)
						.claim("preferred_username", username)
						.claim("realm_access", Map.of("roles", (Object) roles))
						.issuedAt(Instant.now())
						.expiresAt(Instant.now().plusSeconds(3600))
						.build();
				}
			} catch (Exception e) {
				log.debug("No se pudo parsear JWT QA, usando token como subject: {}", e.getMessage());
			}
			return Jwt.withTokenValue(token)
				.header("alg", "none")
				.subject(token)
				.claim("preferred_username", token)
				.claim("realm_access", Map.of("roles", (Object) List.of("USER")))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();
		};
	}
}
