package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.models.Permission;
import com.lk.microservicio_seguridad.models.User;
import com.lk.microservicio_seguridad.Repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class SecurityService {
    @Autowired
    private UserRepository theUserRepository;
    @Autowired
    private EncryptionService theEncryptionService;
    @Autowired
    private JwtService theJwtService;
    @Autowired
    private ValidatorsService theValidatorsService;

    public String login(User theNewUser){
        String token=null;
        User theActualUser=this.theUserRepository.getUserByEmail(theNewUser.getEmail());
        if(theActualUser!=null &&
                theActualUser.getPassword().equals(theEncryptionService.convertSHA256(theNewUser.getPassword()))){
            token=theJwtService.generateToken(theActualUser);

            return token;
        }else{
            return  token;
        }
    }
    public boolean permissionsValidation(final HttpServletRequest request,
                                         Permission thePermission) {
        boolean success=this.theValidatorsService.validationRolePermission(request,thePermission.getUrl(),thePermission.getMethod());
        return success;
    }




}
