package com.lk.microservicio_seguridad.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RecaptchaResponse {
    private boolean success;
    private double score;
    private String action;
    @SerializedName("challenge_ts")
    private String challengeTs;
    private String hostname;
    @SerializedName("error-codes")
    private List<String> errorCodes;

    /**
     * Verifica si la respuesta de reCAPTCHA es válida
     * @param minScore puntuación mínima requerida
     * @param expectedAction acción esperada
     * @return true si la validación es exitosa
     */
    public boolean isValid(double minScore, String expectedAction) {
        return this.success && 
               this.score >= minScore && 
               this.action != null && 
               this.action.equals(expectedAction);
    }
}

