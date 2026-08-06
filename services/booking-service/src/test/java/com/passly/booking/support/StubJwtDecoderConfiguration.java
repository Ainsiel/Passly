package com.passly.booking.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Sustituye el JwtDecoder real (Keycloak) por un stub en los tests de contexto
 * completo que no ejercitan el resource-server. Los subjects {@code user-1} y
 * {@code user-2} actúan como Usuarios.
 */
@TestConfiguration(proxyBeanMethods = false)
public class StubJwtDecoderConfiguration {

	@Bean
	@Primary
	JwtDecoder stubJwtDecoder() {
		return token -> {
			if (token == null || !token.startsWith("user-")) {
				return null;
			}
			return jwtWithSubject(token);
		};
	}

	private static Jwt jwtWithSubject(String subject) {
		return Jwt.withTokenValue("token")
			.header("alg", "none")
			.subject(subject)
			.claim("preferred_username", subject)
			.claim("realm_access", Map.of("roles", List.of("USER")))
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(300))
			.build();
	}
}
