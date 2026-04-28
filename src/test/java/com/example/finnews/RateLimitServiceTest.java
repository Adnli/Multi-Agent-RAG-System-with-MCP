package com.example.finnews;

import com.example.finnews.service.RateLimitService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitServiceTest {
    @Test
    void shouldRejectWhenLimitExceeded() {
        RateLimitService service = new RateLimitService();
        service.check("u1", 2, 60);
        service.check("u1", 2, 60);
        assertThrows(IllegalStateException.class, () -> service.check("u1", 2, 60));
    }
}
