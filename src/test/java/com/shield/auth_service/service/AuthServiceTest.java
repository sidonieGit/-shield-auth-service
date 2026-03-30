package com.shield.auth_service.service;

import com.shield.auth_service.dto.LoginRequest;
import com.shield.auth_service.dto.RegisterRequest;
import com.shield.auth_service.repository.UserRepository;
import com.shield.auth_service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


import com.shield.auth_service.model.User;


import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private TokenService tokenService;

    @Test
    @DisplayName("Devrait réussir l'inscription d'un nouvel utilisateur")
    void registerUser_Success() {
        // ARRANGE : Préparer les données (DTO, User mocké)

        // ACT : Appeler la méthode du service

        // ASSERT : Vérifier que le password a été hashé et l'user sauvegardé
    }

    @Test
    void register_ShouldSaveUser_WhenRequestIsValid() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest("test@shield.com", "password123", "John Doe");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_pwd");

        // ACT
        var response = authService.register(request);

        // ASSERT
        assertNotNull(response);
        assertEquals("Utilisateur enregistré avec succès", response.message());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Login réussi sans MFA - Doit retourner les tokens")
    void login_Success_NoMFA() {
        // ARRANGE
        LoginRequest request = new LoginRequest("test@shield.com", "password");
        User user = User.builder()
                .id("1")
                .email("test@shield.com")
                .password("hashed_pwd")
                .roles(Set.of("ROLE_USER"))
                .mfaEnabled(false)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(tokenService.generateAccessToken(user)).thenReturn("mock_access_token");

        // ACT
        var response = authService.login(request);

        // ASSERT
        assertFalse(response.mfaRequired());
        assertNotNull(response.accessToken());
        assertEquals("mock_access_token", response.accessToken());
    }
}
