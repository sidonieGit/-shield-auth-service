package com.shield.auth_service.service;

import com.shield.auth_service.dto.AuthResponse;
import com.shield.auth_service.dto.LoginRequest;
import com.shield.auth_service.dto.RegisterRequest;
import com.shield.auth_service.model.User;
import com.shield.auth_service.repository.UserRepository;
import com.shield.auth_service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private EmailService emailService;
    @Mock private MfaService mfaService;

    private AuthServiceImpl authService;

    private final String GOOGLE_CLIENT_ID = "test-google-id";

    @BeforeEach
    void setUp() {
        // Instanciation manuelle car on a une String (@Value) dans le constructeur
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                tokenService,
                mfaService,
                GOOGLE_CLIENT_ID,
                emailService
        );
    }

    @Test
    @DisplayName("Inscription - Succès complet (Sauvegarde + Envoi Email)")
    void register_FullSuccess() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest("test@shield.com", "password123", "John Doe");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_pwd");

        // ACT
        AuthResponse response = authService.register(request);

        // ASSERT
        assertNotNull(response);
        // On vérifie que le message correspond à celui de l'implémentation
        assertEquals("Inscription réussie. Vérifiez votre boîte Mailtrap.", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendActivationEmail(eq("test@shield.com"), anyString());
    }

    @Test
    @DisplayName("Login - Doit échouer si le compte n'est pas encore activé")
    void login_ShouldFail_WhenAccountNotActive() {
        // ARRANGE
        LoginRequest request = new LoginRequest("test@shield.com", "password");
        User user = User.builder()
                .email("test@shield.com")
                .active(false) // Compte non activé
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Compte désactivé ou verrouillé", exception.getMessage());
    }

    @Test
    @DisplayName("Login - Succès sans MFA (Retourne les tokens)")
    void login_Success_NoMFA() {
        // ARRANGE
        LoginRequest request = new LoginRequest("test@shield.com", "password");
        User user = User.builder()
                .id("1")
                .email("test@shield.com")
                .password("hashed_pwd")
                .active(true)
                .roles(Set.of("ROLE_USER"))
                .mfaEnabled(false)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(tokenService.generateAccessToken(user)).thenReturn("mock_access_token");

        // ACT
        AuthResponse response = authService.login(request);

        // ASSERT
        assertNotNull(response.getAccessToken());
        assertFalse(response.isMfaRequired());
        assertEquals("mock_access_token", response.getAccessToken());
    }

    @Test
    @DisplayName("Login - Doit demander MFA si activé sur le compte")
    void login_ShouldRequireMFA() {
        // ARRANGE
        LoginRequest request = new LoginRequest("test@shield.com", "password");
        User user = User.builder()
                .email("test@shield.com")
                .password("hashed_pwd")
                .active(true)
                .mfaEnabled(true)
                .mfaSecret("secret")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

        // ACT
        AuthResponse response = authService.login(request);

        // ASSERT
        assertTrue(response.isMfaRequired());
        assertEquals("MFA requis", response.getMessage());
        assertNull(response.getAccessToken()); // Pas de token tant que le MFA n'est pas validé
    }
}