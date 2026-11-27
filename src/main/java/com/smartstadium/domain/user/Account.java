package com.smartstadium.domain.user;

import java.time.Instant;
import java.util.UUID;

/** Login account. Password hashes are never exposed by API DTOs. */
public class Account {
    private final String id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final Instant createdAt;

    public Account(String fullName, String email, String passwordHash, UserRole role) {
        this.id = UUID.randomUUID().toString();
        this.fullName = fullName;
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
