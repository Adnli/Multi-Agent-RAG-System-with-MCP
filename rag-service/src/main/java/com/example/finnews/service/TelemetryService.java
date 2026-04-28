package com.example.finnews.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class TelemetryService {
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer orchestratorTimer;

    public TelemetryService(MeterRegistry meterRegistry) {
        this.successCounter = meterRegistry.counter("finnews.requests.success");
        this.errorCounter = meterRegistry.counter("finnews.requests.error");
        this.orchestratorTimer = meterRegistry.timer("finnews.orchestrator.latency");
    }

    public void success() {
        successCounter.increment();
    }

    public void error() {
        errorCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stop(Timer.Sample sample) {
        sample.stop(orchestratorTimer);
    }
}
