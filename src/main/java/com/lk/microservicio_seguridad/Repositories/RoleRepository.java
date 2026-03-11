package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {
}
