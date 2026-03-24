package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.Repositories.PermissionRepository;
import com.lk.microservicio_seguridad.Repositories.RolePermissionRepository;
import com.lk.microservicio_seguridad.Repositories.RoleRepository;
import com.lk.microservicio_seguridad.models.Permission;
import com.lk.microservicio_seguridad.models.Role;
import com.lk.microservicio_seguridad.models.RolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolePermissionService {

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private PermissionRepository thePermissionRepository;

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    public boolean addRolePermission(String roleId, String permissionId){
        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        Permission permission = this.thePermissionRepository.findById(permissionId).orElse(null);

        if (role != null && permission != null){
            RolePermission theRolePermission = new RolePermission(role, permission);
            this.theRolePermissionRepository.save(theRolePermission);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeRolePermission(String rolePermissionId){
        RolePermission rolePermission = this.theRolePermissionRepository.findById(rolePermissionId).orElse(null);

        if (rolePermission != null){
            this.theRolePermissionRepository.delete(rolePermission);
            return true;
        } else {
            return false;
        }
    }

    public List<RolePermission> getRolePermissionsByRoleId(String roleId) {
        return this.theRolePermissionRepository.getPermissionsByRole(roleId);
    }

    public RolePermission getRolePermission(String roleId, String permissionId) {
        List<RolePermission> rolePermissions = this.theRolePermissionRepository.getPermissionsByRole(roleId);
        for (RolePermission rp : rolePermissions) {
            if (rp.getPermission().getId().equals(permissionId)) {
                return rp;
            }
        }
        return null;
    }
}

