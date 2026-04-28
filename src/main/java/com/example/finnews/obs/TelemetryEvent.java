package com.example.finnews.obs;

import java.time.Instant;

public record TelemetryEvent(String agent, String action, long latencyMs, int tokenEstimate, boolean success, Instant timestamp) {}
