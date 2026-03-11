package com.lk.microservicio_seguridad.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
//Acá se programan las reglas
//Decorador obligatorio en todas las clases, crea el objeto no hay que instanciarla , lo hace el autowrited
@Service
public class UserService {

    //userService necesita a userRepository(inyección), por eso se crea variable de clase que apunta a userRepository
    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private ProfileRepository theProfileRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    public List<User> find(){
        //Le dice al repositorio q aplique el método findAll (lo hace el padre)
        return this.theUserRepository.findAll();
    }

    public User findById(String id){
        //Se le dice al repositorio que busque este elemento por tal id y si no existe que devuelva nulo
        User theUser=this.theUserRepository.findById(id).orElse(null);
        return theUser;
    }

    public User create(User newUser){
        //Le digo al repo el método save. Save se controla de 2 formas: si no existe en la base de datos lo crea y si ya existe lo actualiza
        return this.theUserRepository.save(newUser);
    }

    //Recibe identificador e info que se quiere cambiar
    public User update(String id, User newUser){
        User actualUser=this.theUserRepository.findById(id).orElse(null);

        if(actualUser!=null){
            //Al actualUser que es el q esta en la base de datos se le cambia el nombre con el newUser que es el que llego
            //Se reemplaza atributo por atributo
            //Acá se aplican validaciones, seguridad. En los setters se pueden emitir excepciones
            actualUser.setName(newUser.getName());
            actualUser.setEmail(newUser.getEmail());
            actualUser.setPassword(newUser.getPassword());
            //Se le dice a el repo que guarde esa info en la base de datos
            this.theUserRepository.save(actualUser);
            return actualUser;
        }else{
            return null;
        }
    }

    public void delete(String id){
        //Le digo al repo que busque el user con ese id y si no existe devuelva null
        User theUser=this.theUserRepository.findById(id).orElse(null);
        //Si es diferente de nulo si existe el user y se le dice al repo que aplique el método delete y lo elimina
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