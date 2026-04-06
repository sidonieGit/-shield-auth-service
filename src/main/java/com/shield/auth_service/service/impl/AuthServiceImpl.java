package com.shield.auth_service.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.shield.auth_service.dto.AuthResponse;
import com.shield.auth_service.dto.LoginRequest;
import com.shield.auth_service.dto.RegisterRequest;
import com.shield.auth_service.model.User;
import com.shield.auth_service.repository.UserRepository;
import com.shield.auth_service.service.AuthService;
import com.shield.auth_service.service.EmailService;
import com.shield.auth_service.service.MfaService;
import com.shield.auth_service.service.TokenService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MfaService mfaService;
    private final String googleClientId;
    private final EmailService emailService;
    // Simule une base de jetons temporaires pour le reset password
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, MfaService mfaService, @Value("${google.client-id}") String googleClientId, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mfaService = mfaService;
        this.googleClientId = googleClientId;
        this.emailService = emailService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) throw new RuntimeException("Email déjà utilisé");

        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .roles(Set.of("ROLE_USER"))
                .active(false)
                .verificationToken(token)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);
        emailService.sendActivationEmail(user.getEmail(), token); // Appel à votre EmailServiceImpl

        return AuthResponse.builder()
                .message("Inscription réussie. Vérifiez votre boîte Mailtrap.")
                .build();
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

    // Dans AuthServiceImpl.java

    @Override
    public AuthResponse verifyMfaLogin(String email, int code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification du code TOTP
        if (mfaService.verifyCode(user.getMfaSecret(), code)) {

            // --- CORRECTION : ON REND LE MFA PERMANENT EN BASE ---
            if (!user.isMfaEnabled()) {
                user.setMfaEnabled(true);
                userRepository.save(user);
                log.info("MFA activée de façon permanente pour l'utilisateur : {}", email);
            }

            // On génère un nouveau token qui contiendra maintenant "mfaEnabled: true"
            return AuthResponse.builder()
                    .accessToken(tokenService.generateAccessToken(user))
                    .roles(user.getRoles())
                    .message("MFA vérifié avec succès")
                    .build();
        }
        throw new RuntimeException("Code invalide");
    }

    @Override
    public AuthResponse loginWithGoogle(String idTokenString) {
        // 1. Vérifier le token Google
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) throw new RuntimeException("Token Google invalide");

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // 2. Trouver ou Créer l'utilisateur (Auto-Registration)
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                        .email(email)
                        .fullName(name)
                        .roles(Set.of("ROLE_USER"))
                        .active(true)
                        .createdAt(Instant.now())
                        .build();
                return userRepository.save(newUser);
            });

            // 3. Générer VOTRE Token RSA
            return AuthResponse.builder()
                    .accessToken(tokenService.generateAccessToken(user))
                    .roles(user.getRoles())
                    .message("Connexion Google réussie")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'authentification Google");
        }
    }

    @Override
    public AuthResponse forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email non trouvé"));

        String token = UUID.randomUUID().toString();
        resetTokens.put(token, email);

        emailService.sendResetPasswordEmail(email, token); // Appel à votre EmailServiceImpl
        return AuthResponse.builder().message("Lien de réinitialisation envoyé sur Mailtrap").build();
    }
    @Override
    public AuthResponse updatePassword(String token, String newPassword) {
        String email = resetTokens.get(token);
        if (email == null) throw new RuntimeException("Lien expiré ou invalide");

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetTokens.remove(token); // On consomme le jeton
        return AuthResponse.builder().message("Mot de passe mis à jour avec succès").build();
    }

    @Override
    public AuthResponse disableMfa(String email) {
        // 1. Chercher l'utilisateur
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 2. Désactiver le flag et supprimer le secret (pour des raisons de sécurité)
        user.setMfaEnabled(false);
        user.setMfaSecret(null); // On efface la clé secrète TOTP

        userRepository.save(user);

        return AuthResponse.builder()
                .message("MFA désactivée avec succès")
                .mfaRequired(false)
                .build();
    }

    @Override
    public AuthResponse activateAccount(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Lien d'activation invalide ou expiré"));

        user.setActive(true);
        user.setVerificationToken(null); // On consomme le token-On supprime le token pour qu'il ne serve qu'une fois
        userRepository.save(user);

        // AUTO-LOGIN : On génère le token RSA pour que le front puisse connecter l'user immédiatement
        String accessToken = tokenService.generateAccessToken(user);

        return AuthResponse.builder()
                .message("Compte activé avec succès !Vous pouvez vous connecter")
                .accessToken(accessToken)
                .roles(user.getRoles())
                .build();
    }


}



