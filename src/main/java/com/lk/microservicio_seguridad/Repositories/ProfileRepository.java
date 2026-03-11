package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProfileRepository extends MongoRepository<Profile, String> {
}
