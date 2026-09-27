package com.stylescent.dto;

import java.util.List;

// Formato exacto que el móvil debe mandar en el POST /api/prendas
public record PrendaRequestDTO(
        String nombre,
        Integer idCategoria,
        Integer idColor,
        List<Integer> idEstilos
) {
}
