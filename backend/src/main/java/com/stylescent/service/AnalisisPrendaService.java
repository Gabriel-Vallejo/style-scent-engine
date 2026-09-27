package com.stylescent.service;

import com.stylescent.dto.AnalisisPrendaDTO;
import com.stylescent.dto.SugerenciaDTO;
import com.stylescent.model.Categoria;
import com.stylescent.model.Color;
import com.stylescent.model.Estilo;
import com.stylescent.repository.CategoriaRepository;
import com.stylescent.repository.ColorRepository;
import com.stylescent.repository.EstiloRepository;
import com.stylescent.vision.AnalisisVision;
import com.stylescent.vision.VisionClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Análisis de la foto de una prenda (Fase 2). Solo sugiere: no guarda nada,
// el registro sigue pasando por PrendaService cuando el usuario confirma.
@Service
public class AnalisisPrendaService {

    private final VisionClient visionClient;
    private final CategoriaRepository categoriaRepository;
    private final ColorRepository colorRepository;
    private final EstiloRepository estiloRepository;

    public AnalisisPrendaService(VisionClient visionClient,
                                 CategoriaRepository categoriaRepository,
                                 ColorRepository colorRepository,
                                 EstiloRepository estiloRepository) {
        this.visionClient = visionClient;
        this.categoriaRepository = categoriaRepository;
        this.colorRepository = colorRepository;
        this.estiloRepository = estiloRepository;
    }

    public AnalisisPrendaDTO analizar(byte[] imagen, String nombreArchivo) {
        if (imagen == null || imagen.length == 0) {
            throw new IllegalArgumentException("Debes adjuntar una foto de la prenda");
        }

        // Nombre -> ID: el servicio de visión trabaja con nombres, el formulario con IDs
        Map<String, Integer> categorias = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getNombre, Categoria::getIdCategoria));
        Map<String, Integer> colores = colorRepository.findAll().stream()
                .collect(Collectors.toMap(Color::getNombre, Color::getIdColor));
        Map<String, Integer> estilos = estiloRepository.findAll().stream()
                .collect(Collectors.toMap(Estilo::getNombre, Estilo::getIdEstilo));

        if (categorias.isEmpty() || colores.isEmpty() || estilos.isEmpty()) {
            throw new IllegalArgumentException("El catálogo de categorías, colores o estilos está vacío");
        }

        AnalisisVision analisis = visionClient.analizar(imagen, nombreArchivo,
                List.copyOf(categorias.keySet()), List.copyOf(colores.keySet()), List.copyOf(estilos.keySet()));

        Map<String, Double> confianzaEstilo = analisis.estilos().stream()
                .collect(Collectors.toMap(AnalisisVision.Sugerencia::nombre, AnalisisVision.Sugerencia::confianza));

        List<SugerenciaDTO> estilosSugeridos = analisis.estilosSugeridos().stream()
                .filter(estilos::containsKey)
                .map(nombre -> new SugerenciaDTO(estilos.get(nombre), nombre, confianzaEstilo.getOrDefault(nombre, 0.0)))
                .toList();

        return new AnalisisPrendaDTO(
                mejor(analisis.categorias(), categorias),
                mejor(analisis.colores(), colores),
                estilosSugeridos,
                analisis.colorDominanteHex()
        );
    }

    // Primera sugerencia que exista en el catálogo (debería ser siempre la primera,
    // salvo que alguien haya renombrado un valor mientras se analizaba la foto)
    private static SugerenciaDTO mejor(List<AnalisisVision.Sugerencia> ranking, Map<String, Integer> ids) {
        return ranking.stream()
                .filter(s -> ids.containsKey(s.nombre()))
                .findFirst()
                .map(s -> new SugerenciaDTO(ids.get(s.nombre()), s.nombre(), s.confianza()))
                .orElse(null);
    }
}
