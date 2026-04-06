package com.shield.auth_service.controller;

import com.shield.auth_service.dto.AuthResponse;
import com.shield.auth_service.dto.LoginRequest;
import com.shield.auth_service.dto.MfaVerificationRequest;
import com.shield.auth_service.dto.RegisterRequest;
import com.shield.auth_service.model.User;
import com.shield.auth_service.repository.UserRepository;
import com.shield.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j // Pour les logs de debug
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

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

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("idToken");
        return ResponseEntity.ok(authService.loginWithGoogle(idToken));
    }

    @PostMapping("/password-update")
    public ResponseEntity<AuthResponse> updatePassword(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("password");
        return ResponseEntity.ok(authService.updatePassword(token, newPassword));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@RequestParam String email) {
        return ResponseEntity.ok(authService.forgotPassword(email));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<AuthResponse> disableMfa(@RequestParam String token) {
        return ResponseEntity.ok(authService.disableMfa(token));
    }

    @GetMapping("/activate")
    public ResponseEntity<AuthResponse> activateAccount(@RequestParam String token) {
        return ResponseEntity.ok(authService.activateAccount(token));
    }
    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(Principal principal) {
        // principal.getName() contient l'email extrait du JWT
        return ResponseEntity.ok(userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Profil non trouvé")));
    }
}