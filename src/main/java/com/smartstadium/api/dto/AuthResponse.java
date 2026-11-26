package com.smartstadium.api.dto;

import com.smartstadium.domain.user.UserRole;

public record AuthResponse(
        String token,
        String accountId,
        String fullName,
        String email,
        UserRole role
) {}
