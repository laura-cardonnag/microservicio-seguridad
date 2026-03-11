package com.lk.microservicio_seguridad.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SessionRepository  extends MongoRepository<Session,String> {
}

