package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.Role;
import com.lk.microservicio_seguridad.models.User;
import com.lk.microservicio_seguridad.models.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRoleRepository extends MongoRepository<UserRole,String> {
    boolean existsByUserAndRole(User user, Role role);
}

