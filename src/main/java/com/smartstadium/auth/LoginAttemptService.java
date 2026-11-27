package com.smartstadium.auth;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Locks an email for a while after repeated failed logins (brute-force protection). */
@Service
public class LoginAttemptService {
    static final int MAX_FAILURES = 5;
    static final long LOCK_SECONDS = 300;

    private record State(int failures, Instant lockedUntil) {}

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() { this(Clock.systemUTC()); }

    LoginAttemptService(Clock clock) { this.clock = clock; }

    /** Throws AccountLockedException if the email is currently locked. */
    public void checkAllowed(String email) {
        State s = states.get(key(email));
        if (s != null && s.lockedUntil() != null) {
            Instant now = clock.instant();
            if (now.isBefore(s.lockedUntil())) {
                throw new AccountLockedException(
                        s.lockedUntil().getEpochSecond() - now.getEpochSecond());
            }
            states.remove(key(email));
        }
    }

    public void recordFailure(String email) {
        states.merge(key(email), new State(1, null), (old, ignored) -> {
            int n = old.failures() + 1;
            return n >= MAX_FAILURES
                    ? new State(0, clock.instant().plusSeconds(LOCK_SECONDS))
                    : new State(n, null);
        });
    }

    public void recordSuccess(String email) { states.remove(key(email)); }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
