package com.lk.microservicio_seguridad.Controllers;

import com.lk.microservicio_seguridad.Exceptions.RecaptchaValidationException;
import com.lk.microservicio_seguridad.models.LoginRequest;
import com.lk.microservicio_seguridad.models.RecaptchaResponse;
import com.lk.microservicio_seguridad.models.User;
import com.lk.microservicio_seguridad.Services.AuthService;
import com.lk.microservicio_seguridad.Services.JwtService;
import com.lk.microservicio_seguridad.Services.RecaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtService jwtService;
    private final NotificationController notificationController;
    private final RecaptchaService recaptchaService;

    public AuthController(AuthService authService, JwtService jwtService, 
                         NotificationController notificationController,
                         RecaptchaService recaptchaService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.notificationController = notificationController;
        this.recaptchaService = recaptchaService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");

        User user = authService.register(name, email, password);
        notificationController.sendWelcomeEmail(user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        logger.info("Intento de login para: {}", request.getEmail());

        // Validar que los campos requeridos no sean nulos
        if (request.getEmail() == null || request.getEmail().isEmpty() ||
            request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Email y contraseña son requeridos");
        }

        // Validar el token de reCAPTCHA
        if (request.getRecaptchaToken() == null || request.getRecaptchaToken().isEmpty()) {
            logger.warn("Intento de login sin token de reCAPTCHA: {}", request.getEmail());
            throw new RecaptchaValidationException("Token de reCAPTCHA es requerido");
        }

        // Validar el token contra Google
        RecaptchaResponse recaptchaResponse = recaptchaService.validateLoginToken(request.getRecaptchaToken());

        // Verificar que la validación de reCAPTCHA fue exitosa
        if (!recaptchaResponse.isSuccess()) {
            logger.warn("Validación de reCAPTCHA fallida para: {} - Error: {}", 
                        request.getEmail(), recaptchaResponse.getErrorCodes());
            throw new RecaptchaValidationException("Error en la validación de reCAPTCHA. Por favor, intenta de nuevo.");
        }

        // Verificar el score
        if (recaptchaResponse.getScore() < recaptchaService.getMinScore()) {
            logger.warn("Score de reCAPTCHA muy bajo para: {} - Score: {} - Posible actividad sospechosa",
                        request.getEmail(), recaptchaResponse.getScore());
            throw new RecaptchaValidationException("Validación de seguridad fallida (score bajo). Por favor, intenta de nuevo.");
        }

        // Verificar la acción
        if (!"login".equals(recaptchaResponse.getAction())) {
            logger.error("Acción inesperada en reCAPTCHA para: {} - Action: {}",
                         request.getEmail(), recaptchaResponse.getAction());
            throw new RecaptchaValidationException("Token de reCAPTCHA inválido para esta acción");
        }

        logger.info("✓ reCAPTCHA validado exitosamente para: {} - Score: {}", 
                    request.getEmail(), recaptchaResponse.getScore());

        // Si llegamos aquí, reCAPTCHA fue validado exitosamente, proceder con login normal
        User user = authService.login(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user);

        logger.info("Login exitoso para: {}", request.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }
}