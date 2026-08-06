package com.passly.catalog.security;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extrae los roles del realm de un JWT emitido por Keycloak (claim {@code realm_access.roles}).
 * Es la fuente única del mapeo de claims a roles, compartida por la conversión a autoridades
 * (autorización) y por el resource {@code /me} (exposición de los roles).
 */
public final class RealmRoles {

	private static final String REALM_ACCESS_CLAIM = "realm_access";
	private static final String ROLES_CLAIM = "roles";

	private RealmRoles() {
	}

	public static List<String> from(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
		if (realmAccess == null || !(realmAccess.get(ROLES_CLAIM) instanceof List<?> roleList)) {
			return List.of();
		}
		return roleList.stream()
			.filter(Objects::nonNull)
			.map(Object::toString)
			.toList();
	}
}
