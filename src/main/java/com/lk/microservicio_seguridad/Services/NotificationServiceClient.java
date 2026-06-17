package com.lk.microservicio_seguridad.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceClient.class);
    private static final String NOTIFICATION_SERVICE_URL = "http://localhost:5000/api/enviar-cambio-contrasena";
    
    private final RestTemplate restTemplate;
    
    public NotificationServiceClient() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Envía email de reset de contraseña mediante el servicio de notificaciones
     * 
     * @param email Email del usuario
     * @param userName Nombre del usuario
     * @param resetToken Token de reset
     * @return true si se envió exitosamente, false en caso de error
     */
    public boolean sendPasswordResetEmail(String email, String userName, String resetToken) {
        try {
            // Construir URL del link de reseteo
            String resetLink = "http://localhost:4200/reset-password?token=" + resetToken;
            
            // Construir payload para servicio de notificaciones
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("nombre", userName);
            notificationPayload.put("destinatario", email);
            notificationPayload.put("remitente", "tu-email@gmail.com");
            notificationPayload.put("asunto", "Cambiar tu Contraseña - KALA Buses");
            notificationPayload.put("urlCambio", resetLink);
            notificationPayload.put("tiempoValidez", "30 minutos");
            
            // Llamar al servicio de notificaciones
            try {
                Map<String, Object> response = restTemplate.postForObject(
                    NOTIFICATION_SERVICE_URL,
                    notificationPayload,
                    Map.class
                );
                
                return true;
                
            } catch (RestClientException e) {
                return false;
            }
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Envía email de confirmación de cambio de contraseña
     * 
     * @param email Email del usuario
     * @param userName Nombre del usuario
     * @return true si se envió exitosamente
     */
    public boolean sendPasswordChangeConfirmationEmail(String email, String userName) {
        try {
            // Construir payload para servicio de notificaciones
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("nombre", userName);
            notificationPayload.put("destinatario", email);
            notificationPayload.put("remitente", "tu-email@gmail.com");
            notificationPayload.put("asunto", "Tu contraseña ha sido restablecida exitosamente");
            notificationPayload.put("urlCambio", "");
            notificationPayload.put("tiempoValidez", "");
            
            // Llamar al servicio de notificaciones
            try {
                Map<String, Object> response = restTemplate.postForObject(
                    NOTIFICATION_SERVICE_URL,
                    notificationPayload,
                    Map.class
                );
                
                return true;
                
            } catch (RestClientException e) {
                return false;
            }
            
        } catch (Exception e) {
            return false;
        }
    }
}








