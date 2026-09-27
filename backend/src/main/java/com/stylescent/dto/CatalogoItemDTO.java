package com.stylescent.dto;

// Reutilizable para cualquier tabla de catálogo simple (id + nombre):
// categorías, colores, estilos.
public record CatalogoItemDTO(Integer id, String nombre) {
}
