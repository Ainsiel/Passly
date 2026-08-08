package com.passly.catalog.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
				.requestMatchers(HttpMethod.GET, "/events/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/events").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/events/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/events/**").hasRole("ADMIN")
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	@Lazy
	JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
		if (StringUtils.hasText(jwkSetUri)) {
			NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
			if (StringUtils.hasText(issuerUri)) {
				decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
			}
			return decoder;
		}
		return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new RealmAccessJwtGrantedAuthoritiesConverter());
		return converter;
	}
}
