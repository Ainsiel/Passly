package com.passly.catalog.security;

import java.util.Collection;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Convierte los roles del realm (claim {@code realm_access.roles}) en autoridades {@code ROLE_*}
 * para la autorización de Spring Security. Los roles del realm quedan así expuestos para
 * reglas posteriores (p.ej. {@code hasRole("ADMIN")} en el Catálogo).
 */
public class RealmAccessJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private static final String ROLE_PREFIX = "ROLE_";

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		return RealmRoles.from(jwt).stream()
			.map(ROLE_PREFIX::concat)
			.map(SimpleGrantedAuthority::new)
			.map(GrantedAuthority.class::cast)
			.toList();
	}
}
