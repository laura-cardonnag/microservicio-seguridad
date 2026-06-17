package com.lk.microservicio_seguridad.Services;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class RandomCodeService {

    private static final int CODE_LENGTH = 6;
    private static final int MAX_VALUE = 1_000_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateCode() {
        return String.format("%0" + CODE_LENGTH + "d", SECURE_RANDOM.nextInt(MAX_VALUE));
    }
}

