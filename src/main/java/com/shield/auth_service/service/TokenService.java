package com.shield.auth_service.service;

import com.shield.auth_service.model.User;

public interface TokenService {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
}
