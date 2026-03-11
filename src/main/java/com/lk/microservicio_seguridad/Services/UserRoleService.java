package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.Repositories.RoleRepository;
import com.lk.microservicio_seguridad.Repositories.UserRepository;
import com.lk.microservicio_seguridad.Repositories.UserRoleRepository;
import com.lk.microservicio_seguridad.models.Role;
import com.lk.microservicio_seguridad.models.User;
import com.lk.microservicio_seguridad.models.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {
    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    public boolean addUserRole(String userId,
                               String roleId) {
        User user = this.theUserRepository.findById(userId).orElse(null);
        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        if (user != null && role != null) {
            UserRole theUserRole = new UserRole(user, role);
            this.theUserRoleRepository.save(theUserRole);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeUserRole(String userRoleId) {
        UserRole userRole = this.theUserRoleRepository.findById(userRoleId).orElse(null);
        if (userRole != null) {
            this.theUserRoleRepository.delete(userRole);
            return true;
        } else {
            return false;
        }
    }
}