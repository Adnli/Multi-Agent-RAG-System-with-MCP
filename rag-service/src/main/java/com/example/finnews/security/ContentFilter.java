package com.example.finnews.security;

import java.util.List;

public class ContentFilter {
    private static final List<String> BLOCKED = List.of("drop table", "ignore previous instructions", "exploit", "malware");

    public boolean isAllowed(String text) {
        String lower = text == null ? "" : text.toLowerCase();
        return BLOCKED.stream().noneMatch(lower::contains);
    }
}
