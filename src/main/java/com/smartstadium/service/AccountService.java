package com.smartstadium.service;

import com.smartstadium.domain.user.Account;
import com.smartstadium.domain.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountService {
    private final Map<String, Account> accountsByEmail = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountService(
            @Value("${smartstadium.admin.email:}") String adminEmail,
            @Value("${smartstadium.admin.password:}") String adminPassword) {
        if (!adminEmail.isBlank() && !adminPassword.isBlank()) {
            createAccount("System Administrator", adminEmail, adminPassword, UserRole.ADMIN);
        }
    }

    public Account registerCustomer(String fullName, String email, String password) {
        return createAccount(fullName, email, password, UserRole.CUSTOMER);
    }

    public Account createAccount(String fullName, String email, String password, UserRole role) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || !email.matches("^[^@\\\\s]+@[^@\\\\s]+\\\\.[^@\\\\s]+$")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("Password must contain 8 to 72 characters.");
        }
        String key = normalize(email);
        Account account = new Account(fullName.trim(), key, passwordEncoder.encode(password), role);
        Account existing = accountsByEmail.putIfAbsent(key, account);
        if (existing != null) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        return account;
    }

    public Optional<Account> authenticate(String email, String password) {
        if (email == null || password == null) return Optional.empty();
        Account account = accountsByEmail.get(normalize(email));
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(account);
    }

    public Optional<Account> findByEmail(String email) {
        return Optional.ofNullable(accountsByEmail.get(normalize(email)));
    }

    public long count() {
        return accountsByEmail.size();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
