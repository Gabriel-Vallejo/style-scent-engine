package com.stylescent.dto;

import java.util.List;

// Respuesta de POST /api/prendas/analizar: sugerencias para prerrellenar el
// formulario de registro. El usuario las confirma o corrige antes de guardar.
public record AnalisisPrendaDTO(
        SugerenciaDTO categoria,
        SugerenciaDTO color,
        List<SugerenciaDTO> estilos,
        String colorDominanteHex
) {
}
