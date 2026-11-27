package com.smartstadium.auth;

import com.smartstadium.api.dto.AuthResponse;
import com.smartstadium.api.dto.LoginRequest;
import com.smartstadium.domain.user.Account;
import com.smartstadium.service.AccountService;
import com.smartstadium.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Hardened login at POST /api/auth/secure/login: same result as /api/auth/login
 * but with lockout after repeated failures. Reuses the existing services unchanged.
 */
@RestController
@RequestMapping("/api/auth/secure")
public class AuthSessionController {
    private final AccountService accounts;
    private final TokenService tokens;
    private final LoginAttemptService attempts;

    public AuthSessionController(AccountService accounts, TokenService tokens,
                                 LoginAttemptService attempts) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.attempts = attempts;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        attempts.checkAllowed(request.email());
        Account account = accounts.authenticate(request.email(), request.password())
                .orElseThrow(() -> {
                    attempts.recordFailure(request.email());
                    return new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid email or password.");
                });
        attempts.recordSuccess(request.email());
        return new AuthResponse(tokens.issue(account), account.getId(),
                account.getFullName(), account.getEmail(), account.getRole());
    }

    @PostMapping("/password-check")
    public Map<String, Object> passwordCheck(@RequestBody Map<String, String> body) {
        var problems = PasswordPolicy.violations(body.get("password"));
        return Map.of("valid", problems.isEmpty(), "problems", problems);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> locked(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of("message", ex.getMessage()));
    }
}
