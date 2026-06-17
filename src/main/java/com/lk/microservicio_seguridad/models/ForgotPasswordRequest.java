package com.lk.microservicio_seguridad.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ForgotPasswordRequest {
    private String email;
    private String recaptchaToken;
}

