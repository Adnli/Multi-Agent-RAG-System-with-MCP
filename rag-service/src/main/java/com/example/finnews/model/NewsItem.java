package com.example.finnews.model;

import java.time.Instant;

public record NewsItem(String headline, String source, String content, Instant timestamp) {}
