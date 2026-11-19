package com.vektra.security;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/** Pruebas del límite de intentos fallidos, con un reloj que se adelanta a mano. */
class LoginAttemptLimiterTest {
    private MutableClock clock;
    private LoginAttemptLimiter limiter;
    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        limiter = new LoginAttemptLimiter(clock);
    }
    @Test
    void blocksAfterMaxFailures() {
        for (int i = 1; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure("ana@correo.com");
        }
        assertFalse(limiter.isBlocked("ana@correo.com"));
        limiter.recordFailure("ana@correo.com");
        assertTrue(limiter.isBlocked("ana@correo.com"));
    }
    @Test
    void lockExpiresAfterLockDuration() {
        failMaxTimes("ana@correo.com");
        clock.advance(LoginAttemptLimiter.LOCK_DURATION.minusSeconds(1));
        assertTrue(limiter.isBlocked("ana@correo.com"));
        clock.advance(Duration.ofSeconds(1));
        assertFalse(limiter.isBlocked("ana@correo.com"));
    }
    @Test
    void successResetsTheCounter() {
        for (int i = 1; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure("ana@correo.com");
        }
        limiter.recordSuccess("ana@correo.com");
        limiter.recordFailure("ana@correo.com");
        assertFalse(limiter.isBlocked("ana@correo.com"));
    }
    @Test
    void emailIsComparedWithoutCaseOrSpaces() {
        failMaxTimes(" ANA@Correo.com ");
        assertTrue(limiter.isBlocked("ana@correo.com"));
        assertFalse(limiter.isBlocked("otra@correo.com"));
    }
    @Test
    void remainingLockIsReported() {
        failMaxTimes("ana@correo.com");
        assertEquals(LoginAttemptLimiter.LOCK_DURATION, limiter.remainingLock("ana@correo.com"));
        assertEquals(Duration.ZERO, limiter.remainingLock("otra@correo.com"));
    }
    private void failMaxTimes(String email) {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure(email);
        }
    }
    /** Reloj de prueba: el tiempo solo avanza cuando la prueba lo pide. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T12:00:00Z");
        void advance(Duration duration) {
            now = now.plus(duration);
        }
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }
        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
        @Override
        public Instant instant() {
            return now;
        }
    }
}