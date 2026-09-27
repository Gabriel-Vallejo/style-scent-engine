package com.stylescent.dto;

import java.util.List;

// Respuesta de POST /api/prendas/analizar: sugerencias para prerrellenar el
// formulario de registro. El usuario las confirma o corrige antes de guardar.
public record AnalisisPrendaDTO(
        SugerenciaDTO categoria,
        SugerenciaDTO color,
        // Los siguientes colores más probables: un color apagado o mal iluminado
        // suele estar a medio camino entre dos o tres del catálogo
        List<SugerenciaDTO> alternativasColor,
        List<SugerenciaDTO> estilos,
        String colorDominanteHex
) {
}
