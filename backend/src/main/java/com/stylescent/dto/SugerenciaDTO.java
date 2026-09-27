package com.stylescent.dto;

// Un valor del catálogo propuesto por el análisis de la foto, con su ID ya
// resuelto para que el móvil pueda preseleccionarlo directamente.
public record SugerenciaDTO(Integer id, String nombre, double confianza) {
}
