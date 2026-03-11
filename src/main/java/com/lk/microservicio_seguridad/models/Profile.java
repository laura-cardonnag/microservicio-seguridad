package com.lk.microservicio_seguridad.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Profile {
    @Id
    private String _id;
    private String phone;
    private String photo;

    @DBRef
    private User user;

    public Profile(){

    }

    public Profile(String _id, String phone, String photo) {
        this._id = _id;
        this.phone = phone;
        this.photo = photo;

    }
}