package com.smartstadium.domain.user;

import java.util.Objects;

public class User {
    private final String id;
    private final String fullName;
    private final String email;
    private final UserRole role;

    public User(String id, String fullName, String email, UserRole role) {
        if (id == null || id.isBlank() || fullName == null || fullName.isBlank()
                || email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Valid user id, name and email are required.");
        }
        this.id = id.trim();
        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.role = Objects.requireNonNull(role, "Role is required");
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
}
