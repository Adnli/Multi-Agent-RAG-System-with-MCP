package com.example.finnews.obs;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TelemetryCollector {
    private final List<TelemetryEvent> events = new ArrayList<>();
    private final AtomicInteger failures = new AtomicInteger();

    public synchronized void record(TelemetryEvent event) {
        events.add(event);
        if (!event.success()) {
            failures.incrementAndGet();
        }
    }

    public synchronized double successRate() {
        if (events.isEmpty()) {
            return 1.0;
        }
        long successes = events.stream().filter(TelemetryEvent::success).count();
        return (double) successes / events.size();
    }

    public synchronized double averageLatencyMs() {
        return events.stream().mapToLong(TelemetryEvent::latencyMs).average().orElse(0.0);
    }

    public ResourceUsageSnapshot resourceUsage() {
        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double systemLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        return new ResourceUsageSnapshot(usedMem, systemLoad, failures.get());
    }

    public record ResourceUsageSnapshot(long usedMemoryBytes, double cpuLoadAverage, int failureCount) {}
}
