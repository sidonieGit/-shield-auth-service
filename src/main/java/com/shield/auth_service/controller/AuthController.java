package com.shield.auth_service.controller;

import com.shield.auth_service.dto.AuthResponse;
import com.shield.auth_service.dto.LoginRequest;
import com.shield.auth_service.dto.MfaVerificationRequest;
import com.shield.auth_service.dto.RegisterRequest;
import com.shield.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j // Pour les logs de debug
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Tentative d'inscription pour l'email: {}", request.email());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Tentative de connexion pour l'email: {}", request.email());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/mfa/setup")
    @PreAuthorize("isAuthenticated()") // Seul un utilisateur connecté peut l'activer
    public ResponseEntity<AuthResponse> setupMfa(Principal principal) {
        return ResponseEntity.ok(authService.setupMfa(principal.getName()));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfa(@RequestBody MfaVerificationRequest request) {
        return ResponseEntity.ok(authService.verifyMfaLogin(request.tempToken(), request.code()));
    }

}