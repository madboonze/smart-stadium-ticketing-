package com.smartstadium.service;

import com.smartstadium.domain.user.Account;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final Map<String, Account> activeTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(Account account) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        activeTokens.put(token, account);
        return token;
    }

    public Optional<Account> findAccount(String token) {
        return Optional.ofNullable(activeTokens.get(token));
    }

    public void revoke(String token) {
        if (token != null) activeTokens.remove(token);
    }

    public int activeSessionCount() {
        return activeTokens.size();
    }
}
