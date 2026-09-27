package com.stylescent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Formato exacto que el móvil debe mandar en el POST /api/prendas.
// Los límites de longitud coinciden con las columnas de la BD: sin ellos,
// un nombre largo llegaba a MySQL y el backend respondía 500.
public record PrendaRequestDTO(
        @NotBlank(message = "El nombre de la prenda es obligatorio")
        @Size(max = 100, message = "El nombre de la prenda no puede superar los 100 caracteres")
        String nombre,

        @NotNull(message = "Debes indicar la categoría")
        Integer idCategoria,

        @NotNull(message = "Debes indicar el color")
        Integer idColor,

        @NotEmpty(message = "Debes indicar al menos un estilo")
        List<Integer> idEstilos
) {
}
