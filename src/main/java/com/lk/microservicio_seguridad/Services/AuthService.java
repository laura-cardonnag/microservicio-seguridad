package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.Repositories.UserRepository;
import com.lk.microservicio_seguridad.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public AuthService(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    public User register(String name, String email, String password) {
        if (userRepository.getUserByEmail(email) != null) {
            throw new IllegalArgumentException("Email ya registrado");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encryptionService.convertSHA256(password));

        return userRepository.save(user);
    }

    public User login(String email, String password) {
        logger.info("Intentando login para email: {}", email);

        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            logger.warn("Usuario no encontrado para email: {}", email);
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        logger.info("Usuario encontrado: {}", user.getEmail());

        String hashedPassword = encryptionService.convertSHA256(password);
        if (!hashedPassword.equals(user.getPassword())) {
            logger.warn("Contraseña incorrecta para email: {}", email);
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        logger.info("Contraseña correcta para: {}", email);
        logger.info("Login exitoso para: {}", email);

        return user;
    }
}