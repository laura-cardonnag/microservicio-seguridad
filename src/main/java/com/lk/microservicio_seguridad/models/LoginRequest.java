package com.lk.microservicio_seguridad.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequest {
    private String email;
    private String password;
    private String recaptchaToken;
}

