package com.smartstadium.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AuthModuleTest {
    @Test
    void acceptsNormalEmailsAndRejectsBadOnes() {
        assertTrue(EmailValidator.isValid("john@gmail.com"));
        assertTrue(EmailValidator.isValid("ali.s@stadium.ae"));
        assertFalse(EmailValidator.isValid("no-at-sign.com"));
        assertFalse(EmailValidator.isValid("a@b"));
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    void passwordPolicyRequiresLetterDigitAndLength() {
        assertTrue(PasswordPolicy.isValid("Stadium2026"));
        assertFalse(PasswordPolicy.isValid("short1"));
        assertFalse(PasswordPolicy.isValid("onlyletters"));
        assertFalse(PasswordPolicy.isValid("12345678"));
    }

    @Test
    void locksAfterFiveFailuresThenUnlocks() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        var clock = new MutableClock(t0);
        var svc = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) svc.recordFailure("A@x.com");
        assertThrows(AccountLockedException.class, () -> svc.checkAllowed("a@x.com"));
        clock.now = t0.plusSeconds(LoginAttemptService.LOCK_SECONDS + 1);
        assertDoesNotThrow(() -> svc.checkAllowed("a@x.com"));
    }

    static class MutableClock extends Clock {
        Instant now;
        MutableClock(Instant now) { this.now = now; }
        public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId z) { return this; }
        public Instant instant() { return now; }
    }
}
