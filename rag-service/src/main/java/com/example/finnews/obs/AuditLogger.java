package com.example.finnews.obs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLogger {
    private final List<String> events = new ArrayList<>();

    public synchronized void log(String message) {
        events.add(Instant.now() + " | " + message);
    }

    public synchronized List<String> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}
