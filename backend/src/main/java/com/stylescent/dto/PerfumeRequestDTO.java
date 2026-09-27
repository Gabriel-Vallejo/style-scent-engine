package com.stylescent.dto;

import java.util.List;

// Formato exacto que el móvil debe mandar en el POST /api/perfumes.
// idNotas puede ir vacío: un perfume sin notas registradas simplemente
// no dispara ningún filtro de exclusión.
public record PerfumeRequestDTO(
        String nombre,
        String marca,
        Integer idFamilia,
        Integer idEstado,
        List<Integer> idNotas
) {
}
