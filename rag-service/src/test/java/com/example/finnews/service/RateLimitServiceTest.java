package com.example.finnews.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    @Test
    void allowsRequestsUntilLimitAndRejectsNextOneWithinWindow() {
        RateLimitService service = new RateLimitService();

        assertThatCode(() -> {
            service.check("user-1", 2, 60);
            service.check("user-1", 2, 60);
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> service.check("user-1", 2, 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    void keepsIndependentBucketsPerUser() {
        RateLimitService service = new RateLimitService();

        service.check("user-1", 1, 60);

        assertThatCode(() -> service.check("user-2", 1, 60))
                .doesNotThrowAnyException();
    }
}
