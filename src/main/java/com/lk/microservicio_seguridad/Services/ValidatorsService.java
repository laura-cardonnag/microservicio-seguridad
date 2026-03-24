package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.Repositories.PermissionRepository;
import com.lk.microservicio_seguridad.Repositories.RolePermissionRepository;
import com.lk.microservicio_seguridad.Repositories.UserRepository;
import com.lk.microservicio_seguridad.Repositories.UserRoleRepository;
import com.lk.microservicio_seguridad.models.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValidatorsService {
    @Autowired
    private JwtService jwtService;

    @Autowired
    private PermissionRepository thePermissionRepository;
    @Autowired
    private UserRepository theUserRepository;
    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;
    private static final String BEARER_PREFIX = "Bearer ";

    //Recibe request, url y método, se harán los cruces
    public boolean validationRolePermission(HttpServletRequest request,
                                            String url,
                                            String method) {

        User theUser = this.getUser(request); // 🔴 ahora lanza excepción si falla
        boolean success = false;
        // Normalizar la URL reemplazando: parámetros dinámicos {}, UUIDs, IDs MongoDB y números
        url = url.replaceAll("\\{[^}]+\\}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|[0-9a-fA-F]{24}|\\d+", "?");
        Permission thePermission = this.thePermissionRepository.getPermission(url, method);
        List<UserRole> roles = this.theUserRoleRepository.findByUserId(theUser.getId());
        for (UserRole actual : roles) {
            Role theRole = actual.getRole();

            if (theRole != null && thePermission != null) {
                RolePermission rp = this.theRolePermissionRepository
                        .getRolePermission(theRole.getId(), thePermission.getId());

                if (rp != null) {
                    return true;
                }
            }
        }
        return false; // 🔴 aquí sí es 403
    }
    /*
    Analiza el token y decifra los datos para re armar el usuario
    @param request que contiene el token
    @
    */

    public User getUser(final HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new RuntimeException("Token faltante");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        User theUserFromToken = jwtService.getUserFromToken(token);

        if (theUserFromToken == null) {
            throw new RuntimeException("Token inválido");
        }

        return this.theUserRepository.findById(theUserFromToken.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}

