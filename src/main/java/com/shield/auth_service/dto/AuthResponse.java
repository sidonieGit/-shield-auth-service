package com.shield.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;


@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Cache les champs vides (comme tempToken si pas de MFA)
public record AuthResponse(
        String message,
        boolean mfaRequired,
        String accessToken,
        String refreshToken,
        String tempToken,
        String qrCodeUri,
        Set<String> roles
) {}