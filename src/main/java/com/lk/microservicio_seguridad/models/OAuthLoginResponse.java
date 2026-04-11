package com.lk.microservicio_seguridad.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OAuthLoginResponse {
    private String token;
    private UserInfoResponse user;

    public OAuthLoginResponse(String token, String id, String email, String name, String photoUrl) {
        this.token = token;
        this.user = new UserInfoResponse(id, email, name, photoUrl);
    }

    @Data
    @NoArgsConstructor
    public static class UserInfoResponse {
        private String id;
        private String email;
        private String name;
        private String photoUrl;

        public UserInfoResponse(String id, String email, String name, String photoUrl) {
            this.id = id;
            this.email = email;
            this.name = name;
            this.photoUrl = photoUrl;
        }
    }
}

