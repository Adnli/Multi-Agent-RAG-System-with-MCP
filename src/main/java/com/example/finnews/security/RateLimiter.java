package com.example.finnews.security;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class RateLimiter {
    private final int maxRequests;
    private final long windowSeconds;
    private final Map<String, Deque<Instant>> buckets = new HashMap<>();

    public RateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public synchronized boolean allow(String userId) {
        Instant now = Instant.now();
        Deque<Instant> queue = buckets.computeIfAbsent(userId, k -> new ArrayDeque<>());
        while (!queue.isEmpty() && queue.peekFirst().isBefore(now.minusSeconds(windowSeconds))) {
            queue.pollFirst();
        }
        if (queue.size() >= maxRequests) {
            return false;
        }
        queue.addLast(now);
        return true;
    }
}
