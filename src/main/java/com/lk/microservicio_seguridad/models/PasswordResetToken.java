package com.lk.microservicio_seguridad.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "password_reset_tokens")
@Data
@NoArgsConstructor
public class PasswordResetToken {
    
    @Id
    private String id;
    
    private String userId;
    private String email;
    
    @Indexed(unique = true)
    private String token;
    
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean used;
    private LocalDateTime usedAt;
    
    public PasswordResetToken(String userId, String email, String token, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.userId = userId;
        this.email = email;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = false;
        this.usedAt = null;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    public boolean isValid() {
        return !this.used && !isExpired();
    }
}

