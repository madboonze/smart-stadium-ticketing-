package com.smartstadium.service;

import com.smartstadium.domain.user.Account;
import com.smartstadium.domain.user.UserRole;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentAccountResolver {
    private final TokenService tokenService;

    public CurrentAccountResolver(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Optional<Account> resolve(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) return Optional.empty();
        return tokenService.findAccount(token);
    }

    public Account requireAdmin(String authorizationHeader) {
        Account account = resolve(authorizationHeader)
                .orElseThrow(() -> new SecurityException("Authentication required."));
        if (account.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Administrator access required.");
        }
        return account;
    }
}
