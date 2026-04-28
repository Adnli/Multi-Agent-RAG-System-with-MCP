package com.example.finnews.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class InputPolicyService {
    private static final Set<String> ALLOWED_ROLES = Set.of("student", "analyst", "admin");
    private static final Set<String> BLOCKED_PATTERNS = Set.of("ignore previous instructions", "drop table", "malware", "exploit");

    public String sanitize(String input) {
        return input == null ? "" : input.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
    }

    public String anonymizePii(String text) {
        return text
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[EMAIL]")
                .replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "[SSN]")
                .replaceAll("\\b(?:\\+?1[-. ]?)?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}\\b", "[PHONE]");
    }

    public void validateRole(String role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new SecurityException("Role is not authorized.");
        }
    }

    public void validateContent(String question) {
        String lower = question.toLowerCase();
        for (String blocked : BLOCKED_PATTERNS) {
            if (lower.contains(blocked)) {
                throw new IllegalArgumentException("Blocked by safety filter.");
            }
        }
    }
}
