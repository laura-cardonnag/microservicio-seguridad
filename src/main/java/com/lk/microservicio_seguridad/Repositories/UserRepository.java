package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;


public interface UserRepository extends MongoRepository<User,String>{
    @Query("{'email': ?0}")
    public User getUserByEmail(String email);

}
