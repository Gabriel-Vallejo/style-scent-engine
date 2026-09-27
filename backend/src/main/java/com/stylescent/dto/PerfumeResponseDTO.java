package com.stylescent.dto;

import java.util.List;

public record PerfumeResponseDTO(
        Integer idPerfume,
        String nombre,
        String marca,
        String familia,
        List<String> notas,
        String estado
) {
}
