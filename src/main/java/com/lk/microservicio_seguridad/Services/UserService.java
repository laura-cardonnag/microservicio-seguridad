package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.models.User;
import com.lk.microservicio_seguridad.models.Profile;
import com.lk.microservicio_seguridad.models.Session;
import com.lk.microservicio_seguridad.Repositories.UserRepository;
import com.lk.microservicio_seguridad.Repositories.ProfileRepository;
import com.lk.microservicio_seguridad.Repositories.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private ProfileRepository theProfileRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    public List<User> find(){

        return this.theUserRepository.findAll();
    }

    public User findById(String id){
        User theUser=this.theUserRepository.findById(id).orElse(null);
        return theUser;
    }

    public User create(User newUser){
        return this.theUserRepository.save(newUser);
    }

    public User update(String id, User newUser){
        User actualUser=this.theUserRepository.findById(id).orElse(null);

        if(actualUser!=null){
            actualUser.setName(newUser.getName());
            actualUser.setEmail(newUser.getEmail());
            actualUser.setPassword(newUser.getPassword());
            this.theUserRepository.save(actualUser);
            return actualUser;
        }else{
            return null;
        }
    }

    public void delete(String id){
        User theUser=this.theUserRepository.findById(id).orElse(null);
        if (theUser!=null){
            this.theUserRepository.delete(theUser);
        }
    }

    public boolean addProfile(String userId,String profileId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Profile theProfile=this.theProfileRepository.findById(profileId).orElse(null);
        if(theUser!=null && theProfile!=null){
            theProfile.setUser(theUser);
            this.theProfileRepository.save(theProfile);
            return true;
        }else{
            return false;
        }
    }

    public boolean removeProfile(String userId,String profileId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Profile theProfile=this.theProfileRepository.findById(profileId).orElse(null);
        if(theUser!=null && theProfile!=null){
            theProfile.setUser(null);
            this.theProfileRepository.save(theProfile);
            return true;
        }else{
            return false;
        }
    }

    /**
     * Permite asociar un usuario y una sesión. Para que funcione ambos
     * ya deben de existir en la base de datos
     * @param userId
     * @param sessionId
     * @return
     */
    public boolean addSession(String userId,String sessionId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Session theSession=this.theSessionRepository.findById(sessionId).orElse(null);
        if(theUser!=null && theSession!=null){
            theSession.setUser(theUser);
            this.theSessionRepository.save(theSession);
            return true;
        }else{
            return false;
        }
    }
    public boolean removeSession(String userId,String sessionId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Session theSession=this.theSessionRepository.findById(sessionId).orElse(null);
        if(theUser!=null && theSession!=null){
            theSession.setUser(null);
            this.theSessionRepository.save(theSession);
            return true;
        }else{
            return false;
        }
    }

}