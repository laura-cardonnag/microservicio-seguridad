package com.lk.microservicio_seguridad.Repositories;

import com.lk.microservicio_seguridad.models.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends MongoRepository<Session, String> {
}
