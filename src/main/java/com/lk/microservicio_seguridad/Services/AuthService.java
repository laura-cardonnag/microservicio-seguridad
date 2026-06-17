package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.Repositories.UserRepository;
import com.lk.microservicio_seguridad.Repositories.PasswordResetTokenRepository;
import com.lk.microservicio_seguridad.models.User;
import com.lk.microservicio_seguridad.models.PasswordResetToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EncryptionService encryptionService;
    private final NotificationServiceClient notificationServiceClient;
    private final UserRoleService userRoleService;

    public AuthService(UserRepository userRepository, 
                      PasswordResetTokenRepository resetTokenRepository,
                      EncryptionService encryptionService,
                      NotificationServiceClient notificationServiceClient,
                      UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.encryptionService = encryptionService;
        this.notificationServiceClient = notificationServiceClient;
        this.userRoleService = userRoleService;
    }

    /**
     * Registra un nuevo usuario en el sistema
     * @param name Nombre del usuario
     * @param email Email del usuario
     * @param password Contraseña sin encriptar
     * @return Usuario creado
     */
    public User register(String name, String email, String password) {
        logger.info("📝 Registrando nuevo usuario: {}", email);
        
        // Validar que el usuario no exista
        User existingUser = userRepository.getUserByEmail(email);
        if (existingUser != null) {
            logger.warn("❌ El email {} ya está registrado", email);
            throw new IllegalArgumentException("El email ya está registrado");
        }
        
        // Encriptar contraseña
        String hashedPassword = encryptionService.convertSHA256(password);
        
        // Crear nuevo usuario
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(hashedPassword);
        
        // Guardar en BD
        User savedUser = userRepository.save(user);

        boolean citizenRoleAssigned;
        try {
            citizenRoleAssigned = userRoleService.addCitizenRoleToUser(savedUser.getId());
        } catch (RuntimeException ex) {
            userRepository.delete(savedUser);
            logger.error("❌ Error inesperado al asignar el rol ciudadano al usuario recién registrado: {}", email, ex);
            throw ex;
        }

        if (!citizenRoleAssigned) {
            userRepository.delete(savedUser);
            logger.error("❌ No se pudo asignar el rol ciudadano al usuario recién registrado: {}", email);
            throw new IllegalStateException("No se pudo asignar el rol ciudadano al nuevo usuario");
        }

        logger.info("✅ Usuario registrado exitosamente: {}", email);
        
        return savedUser;
    }

    /**
     * Autentica un usuario con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña sin encriptar
     * @return Usuario autenticado
     */
    public User login(String email, String password) {
        logger.info("🔐 Intentando login para: {}", email);
        
        // Buscar usuario por email
        User user = userRepository.getUserByEmail(email);
        
        if (user == null) {
            logger.warn("❌ Usuario no encontrado: {}", email);
            throw new IllegalArgumentException("Email o contraseña inválidos");
        }
        
        // Encriptar contraseña enviada y comparar
        String hashedPassword = encryptionService.convertSHA256(password);
        
        if (!hashedPassword.equals(user.getPassword())) {
            logger.warn("❌ Contraseña incorrecta para: {}", email);
            throw new IllegalArgumentException("Email o contraseña inválidos");
        }
        
        logger.info("✅ Login exitoso para: {}", email);
        return user;
    }

    /**
     * Autentica/registra un usuario mediante OAuth (Google, Microsoft, GitHub)
     * @param email Email del usuario
     * @param name Nombre del usuario
     * @param provider Proveedor OAuth (google, microsoft, github)
     * @return Usuario autenticado o creado
     */
    public User oauthLogin(String email, String name, String provider) {
        logger.info("🔓 OAuth login recibido: email={}, provider={}", email, provider);
        
        // Buscar usuario existente
        User user = userRepository.getUserByEmail(email);
        
        if (user != null) {
            // Usuario ya existe, actualizar nombre si cambió
            if (name != null && !name.equals(user.getName())) {
                logger.info("📝 Actualizando nombre del usuario: {} → {}", user.getName(), name);
                user.setName(name);
                user = userRepository.save(user);
            }
            logger.info("✅ Usuario existente encontrado y autenticado: {}", email);
            return user;
        }
        
        // Usuario no existe, crear nuevo con contraseña aleatoria
        logger.info("🆕 Creando nuevo usuario OAuth: {}", email);
        
        // Generar contraseña aleatoria
        String randomPassword = UUID.randomUUID().toString();
        String hashedPassword = encryptionService.convertSHA256(randomPassword);
        
        // Crear usuario
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(hashedPassword);
        // Opcionalmente: newUser.setProvider(provider); si existe ese campo
        
        // Guardar en BD
        User savedUser = userRepository.save(newUser);

        boolean oauthCitizenRoleAssigned;
        try {
            oauthCitizenRoleAssigned = userRoleService.addCitizenRoleToUser(savedUser.getId());
        } catch (RuntimeException ex) {
            userRepository.delete(savedUser);
            logger.error("❌ Error inesperado al asignar el rol ciudadano al usuario OAuth recién creado: {}", email, ex);
            throw ex;
        }

        if (!oauthCitizenRoleAssigned) {
            userRepository.delete(savedUser);
            logger.error("❌ No se pudo asignar el rol ciudadano al usuario OAuth recién creado: {}", email);
            throw new IllegalStateException("No se pudo asignar el rol ciudadano al nuevo usuario");
        }

        logger.info("✅ Nuevo usuario OAuth creado: {} (Provider: {})", email, provider);
        
        return savedUser;
    }

    // ...existing code...

    public void initiatePasswordReset(String email) {
        User user = userRepository.getUserByEmail(email);
        
        if (user == null) {
            return;
        }
        
        // Generar token único
        String token = generateResetToken();
        
        // Crear y guardar token en BD
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(RESET_TOKEN_EXPIRY_MINUTES);
        
        PasswordResetToken resetToken = new PasswordResetToken(
            user.getId(),
            email,
            token,
            now,
            expiresAt
        );
        
        resetTokenRepository.save(resetToken);
        
        // Enviar email mediante servicio de notificaciones
        notificationServiceClient.sendPasswordResetEmail(
            email, 
            user.getName(), 
            token
        );
    }

    public void resetPassword(String token, String newPassword) {
        logger.info("🔐 Iniciando reset de contraseña con token: {}", token.substring(0, 8) + "...");
        
        // Validar contraseña
        if (newPassword == null || newPassword.isEmpty()) {
            logger.warn("❌ Contraseña nueva vacía");
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        
        if (newPassword.length() < 8) {
            logger.warn("❌ Contraseña muy corta: {} caracteres", newPassword.length());
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        
        // Validar token
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token);
        
        if (resetToken == null) {
            logger.warn("❌ Token no encontrado en BD");
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        
        if (resetToken.getUsed()) {
            logger.warn("❌ Token ya fue usado: {}", token.substring(0, 8) + "...");
            throw new IllegalArgumentException("Token ya fue utilizado");
        }
        
        if (resetToken.isExpired()) {
            logger.warn("❌ Token expirado: {}", token.substring(0, 8) + "...");
            throw new IllegalArgumentException("Token expirado");
        }
        
        // Buscar usuario
        User user = userRepository.findById(resetToken.getUserId()).orElse(null);
        
        if (user == null) {
            logger.error("❌ Usuario no encontrado para token: {}", token.substring(0, 8) + "...");
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        
        // Actualizar contraseña
        String hashedPassword = encryptionService.convertSHA256(newPassword);
        user.setPassword(hashedPassword);
        userRepository.save(user);
        logger.info("✅ Contraseña actualizada para usuario: {}", user.getEmail());
        
        // Marcar token como usado
        markTokenAsUsed(resetToken);
        logger.info("✅ Token marcado como usado");
        
        // Enviar email de confirmación mediante servicio de notificaciones
        boolean confirmationEmailSent = notificationServiceClient.sendPasswordChangeConfirmationEmail(
            user.getEmail(), 
            user.getName()
        );
        
        if (confirmationEmailSent) {
            logger.info("📧 Email de confirmación enviado a: {}", user.getEmail());
        } else {
            logger.warn("⚠️ No se pudo enviar email de confirmación a: {}", user.getEmail());
        }
        
        logger.info("✨ Reset de contraseña completado exitosamente para: {}", user.getEmail());
    }

    public String generateResetToken() {
        return UUID.randomUUID().toString();
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("⚠️ Token vacío");
            return false;
        }
        
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token);
        
        if (resetToken == null) {
            logger.warn("⚠️ Token no encontrado: {}", token.substring(0, 8) + "...");
            return false;
        }
        
        return resetToken.isValid();
    }

    private void markTokenAsUsed(PasswordResetToken token) {
        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        resetTokenRepository.save(token);
    }

}