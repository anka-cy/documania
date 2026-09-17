package com.documania.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    @Test
    void shouldLimitAfterMaxAttempts() {
        RateLimitService service = new RateLimitService(3, Duration.ofMinutes(10));

        assertFalse(service.isLimited("ip:1.2.3.4"));
        service.record("ip:1.2.3.4");
        service.record("ip:1.2.3.4");
        service.record("ip:1.2.3.4");

        assertTrue(service.isLimited("ip:1.2.3.4"));
        assertFalse(service.isLimited("ip:other"));
    }

    @Test
    void shouldResetCounter() {
        RateLimitService service = new RateLimitService(3, Duration.ofMinutes(10));
        service.record("key");
        service.record("key");
        service.record("key");
        assertTrue(service.isLimited("key"));

        service.reset("key");
        assertFalse(service.isLimited("key"));
    }

    @Test
    void shouldRespectCustomAttemptsAndWindow() {
        RateLimitService service = new RateLimitService(5, Duration.ofMinutes(10));
        service.record("custom", 2, Duration.ofMinutes(10));
        service.record("custom", 2, Duration.ofMinutes(10));

        assertTrue(service.isLimited("custom", 2, Duration.ofMinutes(10)));
        assertFalse(service.isLimited("custom", 5, Duration.ofMinutes(10)));
    }
}