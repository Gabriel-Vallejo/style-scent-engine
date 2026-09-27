package com.stylescent.dto;

// Una entrada del ranking de perfumes para un outfit.
public record RecomendacionDTO(
        PerfumeResponseDTO perfume,
        MatchResultDTO resultado
) {
}
