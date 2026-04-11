package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    
    @Query("{'token': ?0}")
    PasswordResetToken findByToken(String token);
    
    @Query("{'userId': ?0, 'used': false}")
    PasswordResetToken findActiveTokenByUserId(String userId);
}

