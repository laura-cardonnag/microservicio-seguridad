package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRoleRepository extends MongoRepository<UserRole, String> {

    // Opción 1: Query personalizada correcta
    @Query("{ 'user._id' : ?0 }")
    List<UserRole> findByUserId(String userId);

    // Opción 2: Si MongoDB no reconoce, usa el método derivado
    // List<UserRole> findByUserId(String userId);
}