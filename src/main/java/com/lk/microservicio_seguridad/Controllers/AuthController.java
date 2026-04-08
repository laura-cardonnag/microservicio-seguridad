package com.lk.microservicio_seguridad.Controllers;

import com.lk.microservicio_seguridad.Exceptions.RecaptchaValidationException;
import com.lk.microservicio_seguridad.models.LoginRequest;
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
            tempSession.setExpiration(new Date(System.currentTimeMillis() + 600000)); // 10 minutos para verificar

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

            // Retornar solo el sessionId, sin JWT
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Código de verificación enviado a tu correo",
                "sessionId", savedSession.getId()
            ));
        } catch (Exception e) {
            logger.error("✗ Error en login para usuario: {}", request.getEmail(), e);
            throw new RuntimeException("Error interno al procesar el login", e);
        }
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<Map<String, String>> verify2FA(@RequestBody Map<String, String> request) {
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
                return ResponseEntity.status(401).body(Map.of(
                    "success", "false",
                    "message", "Código de verificación incorrecto"
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

            Session finalSession = sessionService.update(sessionId, tempSession);
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
}

