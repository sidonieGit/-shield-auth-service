package com.shield.auth_service.service;

public interface MfaService {
    String generateSecret();
    String getQrCodeUrl(String secret, String email);
    boolean verifyCode(String secret, int code);
}
