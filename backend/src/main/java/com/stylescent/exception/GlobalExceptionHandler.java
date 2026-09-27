package com.stylescent.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// Todas las respuestas de error tienen el mismo formato
// {timestamp, status, error, message}, con un mensaje pensado para mostrarse
// tal cual en la app.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Anotaciones de Bean Validation de los DTOs (@NotBlank, @Size...) con @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(". "));
        return error(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    // p. ej. DELETE /api/perfumes/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' no tiene un formato válido");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleFaltaArchivo(MissingServletRequestPartException ex) {
        return error(HttpStatus.BAD_REQUEST, "Falta el campo '" + ex.getRequestPartName() + "' en la petición");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleArchivoDemasiadoGrande(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "La foto supera los 10 MB");
    }

    // El microservicio de visión no responde: la app puede seguir registrando a mano
    @ExceptionHandler(VisionNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleVisionNoDisponible(VisionNoDisponibleException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                // Map.of no admite null y hay excepciones sin mensaje
                "message", Objects.requireNonNullElse(mensaje, status.getReasonPhrase())
        ));
    }
}
