package com.passly.catalog.auth;

import java.util.List;

public record MeResponse(String username, List<String> roles) {
}
