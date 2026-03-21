package com.lk.microservicio_seguridad.Services;

import com.google.gson.Gson;
import com.lk.microservicio_seguridad.models.RecaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class RecaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(RecaptchaService.class);
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    @Value("${recaptcha.secret-key}")
    private String recaptchaSecretKey;

    @Value("${recaptcha.min-score:0.5}")
    private double recaptchaMinScore;

    public RecaptchaResponse validateToken(String token, String expectedAction) {

        if (token == null || token.isEmpty()) {
            logger.warn("Token de reCAPTCHA vacío");
            RecaptchaResponse response = new RecaptchaResponse();
            response.setSuccess(false);
            return response;
        }

        try {
            // 🔥 DEBUG
            logger.info("SECRET KEY USADA: {}", recaptchaSecretKey);
            logger.info("TOKEN RECIBIDO: {}", token.substring(0, 20) + "...");

            String requestBody = "secret=" + URLEncoder.encode(recaptchaSecretKey, StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RECAPTCHA_VERIFY_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 🔥 DEBUG RESPUESTA
            logger.info("RESPUESTA GOOGLE: {}", response.body());

            RecaptchaResponse recaptchaResponse = gson.fromJson(response.body(), RecaptchaResponse.class);

            logRecaptchaResponse(recaptchaResponse);

            if (!recaptchaResponse.isValid(recaptchaMinScore, expectedAction)) {
                logger.warn("❌ reCAPTCHA inválido - Score: {}, Action: {}",
                        recaptchaResponse.getScore(),
                        recaptchaResponse.getAction());
            }

            return recaptchaResponse;

        } catch (IOException | InterruptedException e) {
            logger.error("Error al comunicarse con Google reCAPTCHA API", e);
            throw new RuntimeException("Error validando reCAPTCHA", e);
        }
    }

    public RecaptchaResponse validateLoginToken(String token) {
        return validateToken(token, "login");
    }

    private void logRecaptchaResponse(RecaptchaResponse response) {
        if (response.isSuccess()) {
            logger.info("✅ reCAPTCHA OK - Score: {}, Action: {}",
                    response.getScore(),
                    response.getAction());
        } else {
            logger.error("❌ Error reCAPTCHA - Codes: {}", response.getErrorCodes());
        }
    }

    public double getMinScore() {
        return recaptchaMinScore;
    }
}