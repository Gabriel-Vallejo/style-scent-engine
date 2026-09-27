package com.stylescent.dto;

import java.util.List;

// Resultado del motor de puntuación para un outfit + un perfume.
// score va acotado a 0-100; scoreSinAcotar permite explicar en la app
// por qué un perfume se queda en 0 o en 100.
public record MatchResultDTO(
        int score,
        int scoreSinAcotar,
        List<DetalleMatchDTO> detalles
) {
}
