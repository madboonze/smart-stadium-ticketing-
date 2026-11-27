package com.smartstadium.auth;

import java.util.ArrayList;
import java.util.List;

/** Password rules: 8-72 chars (BCrypt limit), at least one letter and one digit. */
public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static List<String> violations(String password) {
        List<String> problems = new ArrayList<>();
        if (password == null || password.length() < 8) problems.add("Use at least 8 characters.");
        if (password != null && password.length() > 72) problems.add("Use at most 72 characters.");
        if (password == null || password.chars().noneMatch(Character::isLetter))
            problems.add("Include at least one letter.");
        if (password == null || password.chars().noneMatch(Character::isDigit))
            problems.add("Include at least one digit.");
        return problems;
    }

    public static boolean isValid(String password) {
        return violations(password).isEmpty();
    }
}
