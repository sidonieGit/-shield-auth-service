package com.shield.auth_service.service.impl;

import com.shield.auth_service.dto.AuthResponse;
import com.shield.auth_service.dto.LoginRequest;
import com.shield.auth_service.dto.RegisterRequest;
import com.shield.auth_service.model.User;
import com.shield.auth_service.repository.UserRepository;
import com.shield.auth_service.service.AuthService;
import com.shield.auth_service.service.MfaService;
import com.shield.auth_service.service.TokenService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Builder
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MfaService mfaService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .roles(Set.of("ROLE_USER"))
                .active(true)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        return new AuthResponse("Utilisateur enregistré avec succès", false, null, null, null, null, null);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Identifiants invalides"));

        if (!user.isActive() || user.isAccountLocked()) {
            throw new RuntimeException("Compte désactivé ou verrouillé");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Identifiants invalides");
        }

        // --- Logique MFA ---
        if (user.isMfaEnabled()) {
            // Générer un jeton temporaire pour la validation MFA (Etape suivante)
            String mfaToken = "temp_" + UUID.randomUUID();
            return AuthResponse.builder()
                    .message("MFA requis")
                    .mfaRequired(true)
                    .tempToken(mfaToken)
                    .build();
        }

        // --- Login Direct ---
        String accessToken = tokenService.generateAccessToken(user);

        return AuthResponse.builder()
                .message("Connexion réussie")
                .mfaRequired(false)
                .accessToken(accessToken)
                .roles(user.getRoles())
                .build();
    }

    @Override
    public AuthResponse setupMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        return AuthResponse.builder()
                .message("Scannez le QR Code pour activer le MFA")
                .tempToken(secret) // On envoie le secret pour générer le QR Code au front
                .qrCodeUri(mfaService.getQrCodeUrl(secret, email))
                .build();
    }

    @Override
    public AuthResponse verifyMfaLogin(String tempToken, int code) {
        // 1. Retrouver l'utilisateur via le tempToken (Logique simplifiée pour l'exemple)
        // En prod, utilisez un cache Redis ou un JWT éphémère pour stocker l'ID de l'user
        User user = userRepository.findByMfaSecret(tempToken)
                .orElseThrow(() -> new RuntimeException("Session expirée"));

        if (mfaService.verifyCode(user.getMfaSecret(), code)) {
            String accessToken = tokenService.generateAccessToken(user);
            return AuthResponse.builder()
                    .message("MFA vérifié avec succès")
                    .accessToken(accessToken)
                    .roles(user.getRoles())
                    .build();
        }
        throw new RuntimeException("Code invalide");
    }
}