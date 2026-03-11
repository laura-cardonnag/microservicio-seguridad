package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRoleRepository extends MongoRepository<UserRole, String> {

    @Query("{ 'user.$id' : ObjectId(?0) }")
    public List<UserRole> getRolesByUser(String userId);
}
