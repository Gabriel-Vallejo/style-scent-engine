package com.stylescent.vision;

import java.util.List;

// Respuesta tal cual la devuelve el microservicio de visión (POST /analizar).
// Las listas vienen ordenadas de mayor a menor confianza.
public record AnalisisVision(
        List<Sugerencia> categorias,
        List<Sugerencia> colores,
        List<Sugerencia> estilos,
        List<String> estilosSugeridos,
        String colorDominanteHex,
        String metodoColor
) {
    public record Sugerencia(String nombre, double confianza) {
    }
}
