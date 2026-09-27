package com.stylescent.service;

import com.stylescent.dto.MatchResultDTO;
import com.stylescent.dto.PrendaCreateDTO;
import com.stylescent.model.Categoria;
import com.stylescent.model.Color;
import com.stylescent.model.Estilo;
import com.stylescent.model.FiltroExclusion;
import com.stylescent.model.Nota;
import com.stylescent.model.Perfume;
import com.stylescent.model.Prenda;
import com.stylescent.model.SinergiaColor;
import com.stylescent.model.SinergiaEstilo;
import com.stylescent.repository.CategoriaRepository;
import com.stylescent.repository.ColorRepository;
import com.stylescent.repository.EstiloRepository;
import com.stylescent.repository.FiltroExclusionRepository;
import com.stylescent.repository.PerfumeRepository;
import com.stylescent.repository.PrendaRepository;
import com.stylescent.repository.SinergiaColorRepository;
import com.stylescent.repository.SinergiaEstiloRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class StyleScentService {

    private final PrendaRepository prendaRepository;
    private final PerfumeRepository perfumeRepository;
    private final SinergiaColorRepository sinergiaColorRepository;
    private final SinergiaEstiloRepository sinergiaEstiloRepository;
    private final FiltroExclusionRepository filtroExclusionRepository;

    // Repositorios para registrar prendas
    private final CategoriaRepository categoriaRepository;
    private final ColorRepository colorRepository;
    private final EstiloRepository estiloRepository;

    public StyleScentService(PrendaRepository prendaRepository,
                             PerfumeRepository perfumeRepository,
                             SinergiaColorRepository sinergiaColorRepository,
                             SinergiaEstiloRepository sinergiaEstiloRepository,
                             FiltroExclusionRepository filtroExclusionRepository,
                             CategoriaRepository categoriaRepository,
                             ColorRepository colorRepository,
                             EstiloRepository estiloRepository) {
        this.prendaRepository = prendaRepository;
        this.perfumeRepository = perfumeRepository;
        this.sinergiaColorRepository = sinergiaColorRepository;
        this.sinergiaEstiloRepository = sinergiaEstiloRepository;
        this.filtroExclusionRepository = filtroExclusionRepository;
        this.categoriaRepository = categoriaRepository;
        this.colorRepository = colorRepository;
        this.estiloRepository = estiloRepository;
    }

    private static final int PUNTUACION_BASE = 50;

    /**
     * Calcula el Match Score (0-100) de una combinación de prendas + un perfume.
     */
    public MatchResultDTO calculateMatchScore(List<Integer> prendasIds, Integer perfumeId) {
        Perfume perfume = perfumeRepository.findById(perfumeId)
                .orElseThrow(() -> new EntityNotFoundException("Perfume no encontrado: id " + perfumeId));

        List<Prenda> prendas = prendaRepository.findAllById(prendasIds);

        int score = PUNTUACION_BASE;
        List<String> mensajes = new ArrayList<>();
        mensajes.add("Puntuación base: " + PUNTUACION_BASE);

        // Sinergias de color y estilo, prenda por prenda (usando List para evitar errores de unicidad)
        for (Prenda prenda : prendas) {
            List<SinergiaColor> sinergiasColor =
                    sinergiaColorRepository.findByColorAndFamilia(prenda.getColor(), perfume.getFamilia());

            for (SinergiaColor sinergia : sinergiasColor) {
                int puntos = sinergia.getPuntosSumados();
                score += puntos;
                mensajes.add(String.format("%s combina con perfume %s: %+d puntos",
                        prenda.getColor().getNombre(), perfume.getFamilia().getNombre(), puntos));
            }

            for (Estilo estilo : prenda.getEstilos()) {
                List<SinergiaEstilo> sinergiasEstilo =
                        sinergiaEstiloRepository.findByEstiloAndFamilia(estilo, perfume.getFamilia());

                for (SinergiaEstilo sinergia : sinergiasEstilo) {
                    int puntos = sinergia.getPuntosSumados();
                    score += puntos;
                    mensajes.add(String.format("%s combina con perfume %s: %+d puntos",
                            estilo.getNombre(), perfume.getFamilia().getNombre(), puntos));
                }
            }
        }

        // Filtros de exclusión
        for (Nota nota : perfume.getNotas()) {
            Optional<FiltroExclusion> filtro = filtroExclusionRepository.findByNota(nota);
            if (filtro.isPresent()) {
                int penalizacion = filtro.get().getPenalizacionScore();
                score += penalizacion;
                mensajes.add(String.format("Nota bloqueada '%s': %+d puntos", nota.getNombre(), penalizacion));
            }
        }

        int scoreFinal = Math.max(0, Math.min(100, score));
        return new MatchResultDTO(scoreFinal, mensajes);
    }

    /**
     * Registra una nueva prenda validando sus relaciones en la base de datos.
     */
    @Transactional
    public Prenda registrarPrenda(PrendaCreateDTO dto) {
        Prenda nuevaPrenda = new Prenda();
        nuevaPrenda.setNombre(dto.getNombre());

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Color color = colorRepository.findById(dto.getColorId())
                .orElseThrow(() -> new RuntimeException("Color no encontrado"));

        List<Estilo> estilos = estiloRepository.findAllById(dto.getEstilosIds());

        nuevaPrenda.setCategoria(categoria);
        nuevaPrenda.setColor(color);
        nuevaPrenda.setEstilos(new HashSet<>(estilos));

        return prendaRepository.save(nuevaPrenda);
    }

    // --- MÉTODOS PARA CATÁLOGOS (GET) ---

    public List<Categoria> obtenerCategorias() {
        return categoriaRepository.findAll();
    }

    public List<Color> obtenerColores() {
        return colorRepository.findAll();
    }

    public List<Estilo> obtenerEstilos() {
        return estiloRepository.findAll();
    }

    public List<Prenda> obtenerTodasLasPrendas() {
        return prendaRepository.findAll();
    }

    /**
     * Evalúa todas las prendas de un outfit contra todos los perfumes y devuelve el mejor match.
     */
    public MatchResultDTO recomendarMejorPerfume(List<Integer> prendasIds) {
        List<Perfume> todosLosPerfumes = perfumeRepository.findAll();

        if (todosLosPerfumes.isEmpty()) {
            throw new RuntimeException("No hay perfumes registrados en la base de datos.");
        }

        Perfume mejorPerfume = null;
        int maxScore = -1;
        MatchResultDTO mejorResultado = null;

        for (Perfume perfume : todosLosPerfumes) {
            MatchResultDTO resultado = calculateMatchScore(prendasIds, perfume.getIdPerfume());
            if (resultado.getScore() > maxScore) {
                maxScore = resultado.getScore();
                mejorPerfume = perfume;
                mejorResultado = resultado;
            }
        }

        if (mejorPerfume != null) {
            mejorResultado.getMensajes().add(0, "✨ Perfume recomendado: " + mejorPerfume.getNombre());
        }

        return mejorResultado;
    }
}