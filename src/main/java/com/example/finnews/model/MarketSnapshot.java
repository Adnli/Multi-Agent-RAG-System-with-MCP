package com.example.finnews.model;

import java.time.Instant;

public record MarketSnapshot(String symbol, double price, double dayChangePercent, Instant timestamp) {}
