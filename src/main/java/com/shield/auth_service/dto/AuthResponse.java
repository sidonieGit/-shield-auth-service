package com.shield.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;


@Data
@Builder @NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String message;
    private boolean mfaRequired;
    private boolean mfaEnabled;
    private String accessToken;
    private String refreshToken;
    private String tempToken;
    private String qrCodeUri;
    private Set<String> roles;
}
