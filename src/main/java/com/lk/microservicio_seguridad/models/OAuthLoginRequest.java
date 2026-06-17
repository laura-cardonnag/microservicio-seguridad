package com.lk.microservicio_seguridad.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OAuthLoginRequest {
    private String email;
    private String name;
    private String photoUrl;
    private String provider;
    private String firebaseToken;
}

