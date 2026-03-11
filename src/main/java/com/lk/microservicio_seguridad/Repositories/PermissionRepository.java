package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PermissionRepository extends MongoRepository<Permission, String> {

    @Query("{'url':?0,'method':?1}")
    Permission getPermission(String url,
                             String method);

}
