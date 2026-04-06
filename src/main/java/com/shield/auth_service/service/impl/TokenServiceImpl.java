package com.shield.auth_service.service.impl;

import com.shield.auth_service.model.User;
import com.shield.auth_service.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final JwtEncoder jwtEncoder;

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        // On récupère les rôles pour les injecter dans le claim "roles"
        String scope = user.getRoles().stream()
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shield-auth")
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .subject(user.getEmail())
                .claim("roles", scope)
                .claim("userId", user.getId())
                .claim("mfaEnabled", user.isMfaEnabled())
                .claim("active", user.isActive())
                .claim("fullName", user.getFullName())
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public String generateRefreshToken(User user) {
        return "";
    }
}