package com.smartstadium.api;

import com.smartstadium.api.dto.AccountResponse;
import com.smartstadium.api.dto.AuthResponse;
import com.smartstadium.api.dto.LoginRequest;
import com.smartstadium.api.dto.RegisterRequest;
import com.smartstadium.domain.user.Account;
import com.smartstadium.service.AccountService;
import com.smartstadium.service.CurrentAccountResolver;
import com.smartstadium.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accountService;
    private final TokenService tokenService;
    private final CurrentAccountResolver resolver;

    public AuthController(AccountService accountService, TokenService tokenService,
                          CurrentAccountResolver resolver) {
        this.accountService = accountService;
        this.tokenService = tokenService;
        this.resolver = resolver;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        try {
            Account account = accountService.registerCustomer(
                    request.fullName(), request.email(), request.password());
            return response(account);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Account account = accountService.authenticate(request.email(), request.password())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        return response(account);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false)
                                      String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokenService.revoke(authorization.substring(7).trim());
        }
        return Map.of("message", "Logged out.");
    }

    @GetMapping("/me")
    public AccountResponse me(@RequestHeader(value = "Authorization", required = false)
                              String authorization) {
        Account account = resolver.resolve(authorization)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required."));
        return AccountResponse.from(account);
    }

    private AuthResponse response(Account account) {
        return new AuthResponse(tokenService.issue(account), account.getId(),
                account.getFullName(), account.getEmail(), account.getRole());
    }
}
