package com.smartstadium.api.dto;

import com.smartstadium.domain.user.Account;
import com.smartstadium.domain.user.UserRole;

public record AccountResponse(String id, String fullName, String email, UserRole role) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getFullName(),
                account.getEmail(), account.getRole());
    }
}
