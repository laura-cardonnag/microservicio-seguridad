package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

//Invocaciones a los motores de bases de datos
//Aca se programarian las consultas que se vieron en bases de datos con decorador @
public interface UserRepository extends MongoRepository<User,String> {

    @Query("{'email': ?0}")

    //Mira la coleccion de usuario a ver si está (devuelve usuario, aqui se validaria si el usuario existe)
    public User getUserByEmail(String email);

}
