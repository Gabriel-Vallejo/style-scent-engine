package com.stylescent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Formato exacto que el móvil debe mandar en el POST /api/perfumes.
// idNotas puede ir vacío: un perfume sin notas registradas simplemente
// no dispara ningún filtro de exclusión.
public record PerfumeRequestDTO(
        @NotBlank(message = "El nombre del perfume es obligatorio")
        @Size(max = 100, message = "El nombre del perfume no puede superar los 100 caracteres")
        String nombre,

        @Size(max = 50, message = "La marca no puede superar los 50 caracteres")
        String marca,

        @NotNull(message = "Debes indicar la familia olfativa")
        Integer idFamilia,

        @NotNull(message = "Debes indicar el estado de posesión")
        Integer idEstado,

        List<Integer> idNotas
) {
}
