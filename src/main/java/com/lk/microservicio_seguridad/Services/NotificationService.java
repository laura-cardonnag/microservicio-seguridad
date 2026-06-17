package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

	private final RestTemplate restTemplate;

	@Value("${notification.verification-url:http://localhost:5000/api/enviar-codigo-verificacion}")
	private String verificationUrl;

	@Value("${notification.sender-email:}")
	private String senderEmail;

	public NotificationService() {
		this.restTemplate = new RestTemplate();
	}

	public boolean sendVerificationCode(User user, String code) {
		try {
			Map<String, String> request = new HashMap<>();
			request.put("nombre", user.getName());
			request.put("destinatario", user.getEmail());
			request.put("remitente", (senderEmail != null && !senderEmail.isBlank()) ? senderEmail : user.getEmail());
			request.put("asunto", "Código de Verificación - KALA Buses");
			request.put("codigo", code);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(verificationUrl, entity, String.class);

			logger.info("Código de verificación enviado a {} con respuesta {}", user.getEmail(), response.getStatusCode());
			return response.getStatusCode().is2xxSuccessful();
		} catch (Exception e) {
			logger.error("Error enviando código de verificación a {}", user.getEmail(), e);
			return false;
		}
	}
}
