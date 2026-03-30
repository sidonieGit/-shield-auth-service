package com.shield.auth_service.dto;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

//Document mongodb
@Document(collection = "refresh_tokens")
@Getter @Setter @Builder
public class RefreshToken {
    @Id
    private String id;
    @Indexed(unique = true)
    private String token;
    private String userId;
    private Instant expiryDate;
    private boolean revoked;
}