package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.Repositories.SessionRepository;
import com.lk.microservicio_seguridad.models.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionService {

    @Autowired
    private SessionRepository theSessionRepository;

    public List<Session> find(){
        return (List<Session>) this.theSessionRepository.findAll();
    }

    public Session findById(String id){
        return this.theSessionRepository.findById(id).orElse(null);
    }

    public Session create(Session newSession){
        return this.theSessionRepository.save(newSession);
    }

    public Session update(String id, Session newSession){
        Session actualSession = this.theSessionRepository.findById(id).orElse(null);

        if(actualSession != null){
            actualSession.setToken(newSession.getToken());
            actualSession.setExpiration(newSession.getExpiration());
            actualSession.setCode2FA(newSession.getCode2FA());
            actualSession.setFailedAttempts(newSession.getFailedAttempts());
            this.theSessionRepository.save(actualSession);
            return actualSession;
        } else {
            return null;
        }
    }

    public void delete(String id){
        Session theSession = this.theSessionRepository.findById(id).orElse(null);
        if(theSession != null){
            this.theSessionRepository.delete(theSession);
        }
    }

    public Session incrementFailedAttempts(String id){
        Session actualSession = this.theSessionRepository.findById(id).orElse(null);
        if(actualSession != null){
            Integer attempts = actualSession.getFailedAttempts() == null ? 0 : actualSession.getFailedAttempts();
            actualSession.setFailedAttempts(attempts + 1);
            return this.theSessionRepository.save(actualSession);
        }
        return null;
    }

    public Session reset2FA(String id, String newCode, java.util.Date newExpiration){
        Session actualSession = this.theSessionRepository.findById(id).orElse(null);
        if(actualSession != null){
            actualSession.setCode2FA(newCode);
            actualSession.setExpiration(newExpiration);
            actualSession.setFailedAttempts(0);
            return this.theSessionRepository.save(actualSession);
        }
        return null;
    }
}