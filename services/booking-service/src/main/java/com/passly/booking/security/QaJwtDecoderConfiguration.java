package com.passly.booking.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Decoder JWT para el perfil {@code qa}. Acepta cualquier token Bearer y genera
 * un {@link Jwt} sintético con el token como subject y el rol {@code USER} en
 * {@code realm_access.roles}. Diseñado para load testing con k6: no requiere
 * Keycloak ni validación de firma.
 */
@Configuration
@Profile("qa")
public class QaJwtDecoderConfiguration {

	@Bean
	@Primary
	JwtDecoder qaJwtDecoder() {
		return token -> Jwt.withTokenValue(token)
			.header("alg", "none")
			.subject(token)
			.claim("preferred_username", token)
			.claim("realm_access", Map.of("roles", List.of("USER")))
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(3600))
			.build();
	}
}
