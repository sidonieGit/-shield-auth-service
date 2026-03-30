package com.shield.auth_service.dto;

import java.util.Set;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresAt,
        Set<String> roles
) {}