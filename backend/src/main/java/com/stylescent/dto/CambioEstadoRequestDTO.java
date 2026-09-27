package com.stylescent.dto;

import jakarta.validation.constraints.NotNull;

// Cuerpo del PATCH /api/perfumes/{id}/estado: { "idEstado": 1 }
public record CambioEstadoRequestDTO(
        @NotNull(message = "Debes indicar el nuevo estado")
        Integer idEstado
) {
}
