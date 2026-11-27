package com.smartstadium.auth;

import java.util.regex.Pattern;

/** Correct email check (Module 5). Standalone so no existing file has to change. */
public final class EmailValidator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmailValidator() {}

    public static boolean isValid(String email) {
        return email != null && email.length() <= 160 && EMAIL.matcher(email.trim()).matches();
    }
}
