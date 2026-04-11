package com.lk.microservicio_seguridad.Controllers;

import com.lk.microservicio_seguridad.Exceptions.RecaptchaValidationException;
import com.lk.microservicio_seguridad.models.LoginRequest;
import com.lk.microservicio_seguridad.models.OAuthLoginRequest;
import com.lk.microservicio_seguridad.models.OAuthLoginResponse;
import com.lk.microservicio_seguridad.models.ForgotPasswordRequest;
import com.lk.microservicio_seguridad.models.ResetPasswordRequest;
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
        logger.info("Request recibido: email={}, password={}, recaptchaToken={}", 
                    request.getEmail(), 
                    request.getPassword() != null ? "presente" : "null", 
                    request.getRecaptchaToken() != null ? "presente" : "null");

        // Validar que los campos requeridos no sean nulos
        if (request.getEmail() == null || request.getEmail().isEmpty() ||
            request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Email y contraseña son requeridos");
        }

        logger.info("Campos validados, procediendo con login");

        // TEMPORALMENTE DESHABILITADO PARA PRUEBAS: Validar el token de reCAPTCHA

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
        logger.info("Procediendo con autenticación de usuario");
        User user = authService.login(request.getEmail(), request.getPassword());
        logger.info("Usuario autenticado exitosamente: {}", user.getEmail());

        logger.info("Generando token JWT");
        try {
            String token = jwtService.generateToken(user);
            System.out.println("TOKEN GENERADO: " + token);
            logger.info("Login exitoso para: {}", request.getEmail());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            logger.error("Error generando token JWT para usuario: {}", user.getEmail(), e);
            throw new RuntimeException("Error interno al procesar el login", e);
        }
    }

    @PostMapping("/oauth-login")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthLoginRequest request) {
        logger.info("🔐 OAuth login request recibido");
        logger.info("📧 Email: {}, Name: {}, Provider: {}", 
                    request.getEmail(), request.getName(), request.getProvider());

        try {
            // Validar que los campos requeridos no sean nulos
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                logger.warn("❌ Email es requerido para OAuth login");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email es requerido"));
            }

            if (request.getName() == null || request.getName().isEmpty()) {
                logger.warn("❌ Name es requerido para OAuth login");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Name es requerido"));
            }

            if (request.getProvider() == null || request.getProvider().isEmpty()) {
                logger.warn("❌ Provider es requerido para OAuth login");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Provider es requerido"));
            }

            logger.info("✅ Campos validados correctamente");

            // Procesar OAuth login
            User user = authService.oauthLogin(request.getEmail(), request.getName(), request.getProvider());
            logger.info("✨ Usuario procesado: {} (ID: {})", user.getEmail(), user.getId());

            // Generar JWT token
            logger.info("🔐 Generando JWT token para usuario: {}", user.getEmail());
            String token = jwtService.generateToken(user);
            logger.info("✅ JWT token generado exitosamente");

            // Crear respuesta con token y información del usuario
            OAuthLoginResponse response = new OAuthLoginResponse(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    request.getPhotoUrl() // Solo retornar el photoUrl del request, no de BD
            );

            logger.info("✨ OAuth login exitoso para: {} (Provider: {})", 
                        request.getEmail(), request.getProvider());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error en OAuth login: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error interno al procesar OAuth login",
                                 "details", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            // Validar campos requeridos
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email es requerido"));
            }
            
            if (request.getRecaptchaToken() == null || request.getRecaptchaToken().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "reCAPTCHA token es requerido"));
            }
            
            // Validar reCAPTCHA
            RecaptchaResponse recaptchaResponse = recaptchaService.validateToken(
                request.getRecaptchaToken(), 
                "forgot_password"
            );
            
            if (!recaptchaResponse.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "reCAPTCHA inválido. Por favor, intenta de nuevo."));
            }
            
            // Iniciar proceso de reset
            authService.initiatePasswordReset(request.getEmail());
            
            // RESPUESTA GENÉRICA (por seguridad, siempre igual)
            return ResponseEntity.ok(Map.of(
                "message", "Si el email existe en nuestros registros, recibirá instrucciones de recuperación en su bandeja de entrada."
            ));
            
        } catch (Exception e) {
            // Respuesta genérica incluso en caso de error (por seguridad)
            return ResponseEntity.ok(Map.of(
                "message", "Si el email existe en nuestros registros, recibirá instrucciones de recuperación en su bandeja de entrada."
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        logger.info("🔐 Solicitud de reset de contraseña recibida");
        
        try {
            // Validar campos requeridos
            if (request.getToken() == null || request.getToken().isEmpty()) {
                logger.warn("❌ Token es requerido para reset-password");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Token es requerido"));
            }
            
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                logger.warn("❌ Nueva contraseña es requerida");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Nueva contraseña es requerida"));
            }
            
            if (request.getNewPassword().length() < 8) {
                logger.warn("❌ Nueva contraseña muy corta");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La contraseña debe tener al menos 8 caracteres"));
            }
            
            logger.info("✅ Validaciones de entrada completadas");
            
            // Procesar reset de contraseña
            authService.resetPassword(request.getToken(), request.getNewPassword());
            
            logger.info("✨ Reset de contraseña completado exitosamente");
            
            return ResponseEntity.ok(Map.of(
                "message", "Contraseña restablecida exitosamente. Puede iniciar sesión con su nueva contraseña."
            ));
            
        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Error en reset-password: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Error inesperado en reset-password: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error interno al procesar el reset de contraseña"));
        }
    }
}