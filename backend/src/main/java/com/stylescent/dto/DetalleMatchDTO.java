package com.stylescent.dto;

// Cada suma o resta concreta del Match Score, con su origen:
// BASE (puntuación inicial), COLOR / ESTILO (sinergias de una prenda)
// o EXCLUSION (nota vetada del perfume).
public record DetalleMatchDTO(
        String tipo,
        String descripcion,
        int puntos
) {
}
