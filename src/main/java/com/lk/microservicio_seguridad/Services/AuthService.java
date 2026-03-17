package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.Repositories.UserRepository;
import com.lk.microservicio_seguridad.models.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

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
        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        String hashedPassword = encryptionService.convertSHA256(password);
        if (!hashedPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        return user;
    }
}