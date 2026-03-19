package com.lk.microservicio_seguridad.Controllers;

import com.lk.microservicio_seguridad.models.User;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationController {

    private final RestTemplate restTemplate;

    public NotificationController() {
        this.restTemplate = new RestTemplate();
    }

    public void sendWelcomeEmail(User user) {
        String url = "http://localhost:5000/api/enviar-html";

        Map<String, String> request = new HashMap<>();
        request.put("nombre", user.getName());
        request.put("destinatario", user.getEmail());
        request.put("remitente", user.getEmail());
        request.put("asunto", "Bienvenido a KALA Buses");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            // Opcional: loggear la respuesta
            System.out.println("Email enviado: " + response.getStatusCode());
        } catch (Exception e) {
            // Manejar error
            System.err.println("Error enviando email: " + e.getMessage());
        }
    }
}
