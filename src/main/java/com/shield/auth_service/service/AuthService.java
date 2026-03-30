package com.shield.auth_service.service;

import com.shield.auth_service.dto.AuthResponse;
import com.shield.auth_service.dto.LoginRequest;
import com.shield.auth_service.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse setupMfa(String email);

    AuthResponse verifyMfaLogin(String tempToken, int code);
}