package com.lk.microservicio_seguridad.Controllers;

import com.lk.microservicio_seguridad.Exceptions.RecaptchaValidationException;
import com.lk.microservicio_seguridad.models.LoginRequest;
import com.lk.microservicio_seguridad.models.OAuthLoginRequest;
import com.lk.microservicio_seguridad.models.OAuthLoginResponse;
import com.lk.microservicio_seguridad.models.ForgotPasswordRequest;
import com.lk.microservicio_seguridad.models.ResetPasswordRequest;
import com.lk.microservicio_seguridad.models.RecaptchaResponse;
import com.lk.microservicio_seguridad.models.Session;
import com.lk.microservicio_seguridad.models.User;
import com.lk.microservicio_seguridad.Services.AuthService;
import com.lk.microservicio_seguridad.Services.JwtService;
import com.lk.microservicio_seguridad.Services.RandomCodeService;
import com.lk.microservicio_seguridad.Services.NotificationService;
import com.lk.microservicio_seguridad.Services.RecaptchaService;
import com.lk.microservicio_seguridad.Services.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Date;

@RestController
@RequestMapping("/api/public/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final int MAX_2FA_ATTEMPTS = 3;
    private static final long TWO_FA_EXPIRATION_MS = 10 * 60 * 1000L;

    private final AuthService authService;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final RandomCodeService randomCodeService;
    private final NotificationService notificationService;
    private final NotificationController notificationController;
    private final RecaptchaService recaptchaService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    public AuthController(AuthService authService, JwtService jwtService, 
                          SessionService sessionService,
                          RandomCodeService randomCodeService,
                          NotificationService notificationService,
                         NotificationController notificationController,
                         RecaptchaService recaptchaService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.randomCodeService = randomCodeService;
        this.notificationService = notificationService;
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
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
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

        // Si llegamos aquí, reCAPTCHA fue validado exitosamente
        logger.info("Procediendo con autenticación de usuario");
        User user = authService.login(request.getEmail(), request.getPassword());
        logger.info("Usuario autenticado exitosamente: {}", user.getEmail());

        try {
            // PASO 1: Crear sesión temporal (sin token JWT) con código 2FA
            Session tempSession = new Session();
            String generatedCode = randomCodeService.generateCode();
            tempSession.setCode2FA(generatedCode);
            tempSession.setUser(user);
            tempSession.setExpiration(new Date(System.currentTimeMillis() + TWO_FA_EXPIRATION_MS)); // 10 minutos para verificar

            logger.info("📋 [CÓDIGO GENERADO] - Usuario: {} | Código: '{}' | Longitud: {}",
                user.getEmail(), generatedCode, generatedCode.length());

            Session savedSession = sessionService.create(tempSession);
            logger.info("✓ Sesión temporal creada para {}. ID: {}", user.getEmail(), savedSession.getId());
            logger.info("📋 [CÓDIGO EN SESIÓN] - SessionID: {} | Código almacenado: '{}' | Usuario: {}",
                savedSession.getId(), savedSession.getCode2FA(), user.getEmail());

            // PASO 2: Enviar código 2FA por correo
            logger.info("📧 [ANTES DE ENVIAR CORREO] - Usuario: {} | Código a enviar: '{}' | SessionID: {}",
                user.getEmail(), savedSession.getCode2FA(), savedSession.getId());

            boolean codeSent = notificationService.sendVerificationCode(savedSession.getUser(), savedSession.getCode2FA());

            if (!codeSent) {
                logger.error("✗ No se pudo enviar el código de verificación para: {}", user.getEmail());
                throw new RuntimeException("No se pudo enviar el código de verificación. Por favor intenta de nuevo.");
            }

            logger.info("✓ Código 2FA enviado exitosamente a: {} | Código enviado: '{}' | SessionID: {}",
                user.getEmail(), savedSession.getCode2FA(), savedSession.getId());
            logger.info("✓ Aguardando verificación 2FA para: {}", user.getEmail());

            String maskedEmail = maskEmail(user.getEmail());

            // Retornar solo el sessionId, sin JWT
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Código de verificación enviado a tu correo",
                "sessionId", savedSession.getId(),
                "maskedEmail", maskedEmail,
                "expiresAt", String.valueOf(savedSession.getExpiration().getTime()),
                "attemptsRemaining", String.valueOf(MAX_2FA_ATTEMPTS)
            ));
        } catch (Exception e) {
            logger.error("✗ Error en login para usuario: {}", request.getEmail(), e);
            throw new RuntimeException("Error interno al procesar el login", e);
        }
    }

<<<<<<< HEAD
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
=======
    @PostMapping("/verify-2fa")
    public ResponseEntity<Map<String, Object>> verify2FA(@RequestBody Map<String, String> request) {
        logger.info("Solicitud de verificación 2FA para sesión: {}", request.get("sessionId"));

        String sessionId = request.get("sessionId");
        String code2FA = request.get("code2FA");

        // Validar campos requeridos
        if (sessionId == null || sessionId.isEmpty()) {
            logger.warn("Solicitud de verificación 2FA sin sessionId");
            return ResponseEntity.status(400).body(Map.of(
                "success", "false",
                "message", "sessionId es requerido"
            ));
        }

        if (code2FA == null || code2FA.isEmpty()) {
            logger.warn("Solicitud de verificación 2FA sin código para sesión: {}", sessionId);
            return ResponseEntity.status(400).body(Map.of(
                "success", "false",
                "message", "Código 2FA es requerido"
            ));
        }

        try {
            // Obtener la sesión temporal
            Session tempSession = sessionService.findById(sessionId);

            if (tempSession == null) {
                logger.warn("✗ Sesión no encontrada: {}", sessionId);
                return ResponseEntity.status(404).body(Map.of(
                    "success", "false",
                    "message", "Sesión no encontrada o expirada"
                ));
            }

            logger.info("Sesión encontrada para usuario: {}", tempSession.getUser().getEmail());
            logger.info("📋 [CÓDIGO EN VERIFICACIÓN] - SessionID: {} | Código almacenado: '{}' | Código recibido: '{}' | Usuario: {}",
                sessionId, tempSession.getCode2FA(), code2FA, tempSession.getUser().getEmail());

            // Verificar que la sesión no haya expirado
            if (tempSession.getExpiration().before(new Date())) {
                logger.warn("✗ Sesión expirada para: {}", sessionId);
                logger.warn("⏰ [SESIÓN EXPIRADA] - SessionID: {} | Expiración: {} | Hora actual: {}",
                    sessionId, tempSession.getExpiration(), new Date());
                sessionService.delete(sessionId);
                return ResponseEntity.status(401).body(Map.of(
                    "success", "false",
                    "message", "Código inválido o expirado. Inténtalo nuevamente."
                ));
            }

            // Verificar que el código no sea nulo
            if (tempSession.getCode2FA() == null) {
                logger.error("✗ El código 2FA en sesión es nulo para: {}", sessionId);
                logger.error("🔴 [ERROR CRÍTICO] - SessionID: {} | El código almacenado es NULL", sessionId);
                return ResponseEntity.status(500).body(Map.of(
                    "success", "false",
                    "message", "Error interno al verificar el código"
                ));
            }

            logger.info("🔍 [COMPARACIÓN DE CÓDIGOS]");
            logger.info("   📌 Código almacenado: '{}' (tipo: String, longitud: {})",
                tempSession.getCode2FA(), tempSession.getCode2FA().length());
            logger.info("   📌 Código recibido: '{}' (tipo: String, longitud: {})",
                code2FA, code2FA.length());
            logger.info("   📌 ¿Son iguales? {}", tempSession.getCode2FA().equals(code2FA));

            // Verificar si el código coincide
            if (!tempSession.getCode2FA().equals(code2FA)) {
                logger.warn("✗ Código 2FA inválido para sesión: {}. Esperado: '{}', Recibido: '{}'",
                    sessionId, tempSession.getCode2FA(), code2FA);
                logger.error("❌ [VERIFICACIÓN FALLIDA] - SessionID: {} | Usuario: {} | Código correcto: '{}' | Código ingresado: '{}'",
                    sessionId, tempSession.getUser().getEmail(), tempSession.getCode2FA(), code2FA);
                Session updatedSession = sessionService.incrementFailedAttempts(sessionId);
                int attempts = updatedSession != null && updatedSession.getFailedAttempts() != null ? updatedSession.getFailedAttempts() : 0;
                int remainingAttempts = Math.max(0, MAX_2FA_ATTEMPTS - attempts);

                if (attempts >= MAX_2FA_ATTEMPTS) {
                    sessionService.delete(sessionId);
                    return ResponseEntity.status(401).body(Map.of(
                        "success", "false",
                        "message", "Código incorrecto. Intentos agotados. Vuelve a iniciar sesión.",
                        "attemptsRemaining", 0
                    ));
                }

                return ResponseEntity.status(401).body(Map.of(
                    "success", "false",
                    "message", "Código incorrecto. Intentos restantes: " + remainingAttempts,
                    "attemptsRemaining", remainingAttempts
                ));
            }

            logger.info("✓ Código 2FA válido para usuario: {}", tempSession.getUser().getEmail());
            logger.info("✅ [VERIFICACIÓN EXITOSA] - SessionID: {} | Usuario: {} | Código: '{}' | Validado correctamente",
                sessionId, tempSession.getUser().getEmail(), code2FA);

            // PASO 1: Generar JWT
            String token = jwtService.generateToken(tempSession.getUser());
            logger.info("✓ Token JWT generado para: {}", tempSession.getUser().getEmail());

            // PASO 2: Actualizar la sesión con el token JWT
            tempSession.setToken(token);
            tempSession.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration));
            tempSession.setFailedAttempts(0);

            sessionService.update(sessionId, tempSession);
            logger.info("✓ Sesión actualizada con token JWT para: {}", tempSession.getUser().getEmail());

            // Código verificado exitosamente
            logger.info("✓ Verificación 2FA exitosa para usuario: {}", tempSession.getUser().getEmail());
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Verificación 2FA exitosa",
                "userId", tempSession.getUser().getId(),
                "email", tempSession.getUser().getEmail(),
                "token", token
            ));

        } catch (Exception e) {
            logger.error("✗ Error en verificación 2FA para sesión: {}", sessionId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", "false",
                "message", "Error interno al verificar el código"
            ));
        }
    }

    @PostMapping("/resend-2fa")
    public ResponseEntity<Map<String, Object>> resend2FA(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");

        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of(
                "success", "false",
                "message", "sessionId es requerido"
            ));
        }

        Session session = sessionService.findById(sessionId);
        if (session == null) {
            return ResponseEntity.status(404).body(Map.of(
                "success", "false",
                "message", "Sesión no encontrada o expirada"
            ));
        }

        if (session.getExpiration() != null && session.getExpiration().before(new Date())) {
            sessionService.delete(sessionId);
            return ResponseEntity.status(401).body(Map.of(
                "success", "false",
                "message", "Código inválido o expirado. Inténtalo nuevamente."
            ));
        }

        String newCode = randomCodeService.generateCode();
        Date newExpiration = new Date(System.currentTimeMillis() + TWO_FA_EXPIRATION_MS);
        Session updated = sessionService.reset2FA(sessionId, newCode, newExpiration);

        if (updated == null) {
            return ResponseEntity.status(404).body(Map.of(
                "success", "false",
                "message", "Sesión no encontrada o expirada"
            ));
        }

        boolean codeSent = notificationService.sendVerificationCode(updated.getUser(), newCode);
        if (!codeSent) {
            return ResponseEntity.status(500).body(Map.of(
                "success", "false",
                "message", "No se pudo reenviar el código"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Código reenviado",
            "sessionId", updated.getId(),
            "maskedEmail", maskEmail(updated.getUser().getEmail()),
            "expiresAt", String.valueOf(newExpiration.getTime()),
            "attemptsRemaining", String.valueOf(MAX_2FA_ATTEMPTS)
        ));
    }

    @PostMapping("/cancel-2fa")
    public ResponseEntity<Map<String, Object>> cancel2FA(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");

        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of(
                "success", "false",
                "message", "sessionId es requerido"
            ));
        }

        sessionService.delete(sessionId);
        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Sesión 2FA invalidada"
        ));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@", 2);
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return "**@" + domain;
        }

        String visible = username.substring(0, Math.min(3, username.length()));
        return visible + "***@***." + (domain.contains(".") ? domain.substring(domain.lastIndexOf('.') + 1) : domain);
    }
}

>>>>>>> 549373e121605a17e1741d934e80db6d9f26f80d
