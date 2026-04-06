package com.shield.auth_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Set;

@Document(collection = "users")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class User {
    @Id
    private String id;

    @Indexed(unique = true) // Créé automatiquement au démarrage
    private String email;

    private String password; // Hashé (BCrypt)

    private String fullName;

    private Set<String> roles; // ROLE_USER, ROLE_ADMIN, etc.


    // --- Statut du compte ---
    private boolean active = false;
    private boolean accountLocked = false;
    // --- Gestion MFA ---
    private boolean mfaEnabled = false;
    private String mfaSecret; // Pour Google Authenticator


    private String verificationToken;
    private Instant createdAt;
    private Instant lastLogin;

}