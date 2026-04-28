package com.example.finnews.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Service
public class RateLimitService {
    private final Map<String, Deque<Instant>> buckets = new HashMap<>();

    public synchronized void check(String userId, int maxReq, int windowSec) {
        Instant now = Instant.now();
        Deque<Instant> q = buckets.computeIfAbsent(userId, x -> new ArrayDeque<>());
        while (!q.isEmpty() && q.peekFirst().isBefore(now.minusSeconds(windowSec))) {
            q.pollFirst();
        }
        if (q.size() >= maxReq) {
            throw new IllegalStateException("Rate limit exceeded");
        }
        q.addLast(now);
    }
}
