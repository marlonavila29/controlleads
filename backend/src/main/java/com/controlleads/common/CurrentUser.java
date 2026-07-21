package com.controlleads.common;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Identity of the caller, extracted from the JWT. */
public record CurrentUser(UUID id, String role) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }

    public boolean isAdmin() {
        return "ADMINISTRATOR".equals(role);
    }
}
