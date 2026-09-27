package com.stylescent.dto;

import java.util.List;

// Lo que el backend devuelve tras registrar/listar una prenda.
// Nunca devolvemos la entidad Prenda directamente para evitar problemas
// de serialización con las relaciones JPA (lazy loading, referencias circulares).
public record PrendaResponseDTO(
        Integer idPrenda,
        String nombre,
        String categoria,
        String color,
        List<String> estilos,
        String fechaRegistro
) {
}
