package com.passly.catalog.auth;

import java.util.List;

import com.passly.catalog.security.RealmRoles;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		String username = jwt.getClaimAsString("preferred_username");
		List<String> roles = RealmRoles.from(jwt);
		return new MeResponse(username, roles);
	}
}
