package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface RolePermissionRepository extends MongoRepository<RolePermission,String> {

    @Query("{'role.$id': ObjectId(?0)}")
    List<RolePermission> getPermissionsByRole(String roleId);
    @Query("{'role.$id': ObjectId(?0),'permission.$id': ObjectId(?1)}")
    public RolePermission getRolePermission(String roleId,String permissionId);

}

