package com.shield.auth_service.service;

public interface EmailService {
    void sendActivationEmail(String email, String token);
    void sendResetPasswordEmail(String email, String token);
}
