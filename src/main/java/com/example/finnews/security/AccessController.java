package com.example.finnews.security;

import java.util.Set;

public class AccessController {
    private static final Set<String> ALLOWED_ROLES = Set.of("analyst", "student", "admin");

    public boolean isAuthorized(String role) {
        return ALLOWED_ROLES.contains(role);
    }
}
