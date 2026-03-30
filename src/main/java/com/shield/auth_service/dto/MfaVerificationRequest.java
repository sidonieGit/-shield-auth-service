package com.shield.auth_service.dto;

public record MfaVerificationRequest(String tempToken, int code) {
}
