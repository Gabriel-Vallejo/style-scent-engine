package com.stylescent.dto;

// Cuerpo del PATCH /api/perfumes/{id}/estado: { "idEstado": 1 }
public record CambioEstadoRequestDTO(Integer idEstado) {
}
