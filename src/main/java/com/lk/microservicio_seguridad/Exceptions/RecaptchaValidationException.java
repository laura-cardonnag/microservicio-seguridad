package com.lk.microservicio_seguridad.Exceptions;

public class RecaptchaValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RecaptchaValidationException(String message) {
        super(message);
    }

    public RecaptchaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

