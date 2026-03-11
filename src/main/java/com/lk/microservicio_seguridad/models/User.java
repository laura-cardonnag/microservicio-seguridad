package com.lk.microservicio_seguridad.models;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

//Decoradores
@Document //quiere decir que esta entidad será almacenada en mongoDB (quiere decir que esto será una colección o tabla en bases datos no relacional)
@Data //getters y setters no necesarios, ya los crea el decorador de lombok (lo hace en tiempo de ejecución)
public class User {
    //Como id es la clave primaria debe ir con decorador, para decir que será identificada con ese atributo
    @Id
    private String id;
    private String name;
    private String password;
    private String email;

    //Constructor (no es necesario pedir el id al usuario, por eso se seleccionan el resto de atributos)
    //click derecho y en generate constructor
    public User(String name, String password, String email) {
        this.name = name;
        this.password = password;
        this.email = email;
    }

    //Constructor de usuario vació porque algunas veces se necesitan crear usuarios en blanco
    public User() {}


}