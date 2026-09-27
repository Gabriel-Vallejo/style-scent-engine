package com.stylescent.exception;

// El microservicio de visión no responde (parado, arrancando, error interno).
// GlobalExceptionHandler la convierte en 503: la app puede seguir registrando a mano.
public class VisionNoDisponibleException extends RuntimeException {

    public VisionNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
