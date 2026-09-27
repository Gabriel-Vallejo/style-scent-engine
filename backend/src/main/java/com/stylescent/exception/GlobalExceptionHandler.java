package com.stylescent.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.NOT_FOUND.value(),
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(VisionNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleVisionNoDisponible(VisionNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleArchivoDemasiadoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.CONTENT_TOO_LARGE.value(),
                "error", "Content Too Large",
                "message", "La foto supera los 10 MB"
        ));
    }
}
