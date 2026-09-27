package com.stylescent.service;

import com.stylescent.dto.MatchResultDTO;
import com.stylescent.model.Estilo;
import com.stylescent.model.FiltroExclusion;
import com.stylescent.model.Nota;
import com.stylescent.model.Perfume;
import com.stylescent.model.Prenda;
import com.stylescent.model.SinergiaColor;
import com.stylescent.model.SinergiaEstilo;
import com.stylescent.repository.FiltroExclusionRepository;
import com.stylescent.repository.PerfumeRepository;
import com.stylescent.repository.PrendaRepository;
import com.stylescent.repository.SinergiaColorRepository;
import com.stylescent.repository.SinergiaEstiloRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StyleScentService {

    private final PrendaRepository prendaRepository;
    private final PerfumeRepository perfumeRepository;
    private final SinergiaColorRepository sinergiaColorRepository;
    private final SinergiaEstiloRepository sinergiaEstiloRepository;
    private final FiltroExclusionRepository filtroExclusionRepository;

    public StyleScentService(PrendaRepository prendaRepository,
                              PerfumeRepository perfumeRepository,
                              SinergiaColorRepository sinergiaColorRepository,
                              SinergiaEstiloRepository sinergiaEstiloRepository,
                              FiltroExclusionRepository filtroExclusionRepository) {
        this.prendaRepository = prendaRepository;
        this.perfumeRepository = perfumeRepository;
        this.sinergiaColorRepository = sinergiaColorRepository;
        this.sinergiaEstiloRepository = sinergiaEstiloRepository;
        this.filtroExclusionRepository = filtroExclusionRepository;
    }

    private static final int PUNTUACION_BASE = 50;

    /**
     * Calcula el Match Score (0-100) de una combinación de prendas + un perfume.
     * Parte de una base de 50 puntos, suma sinergias de color/estilo de cada
     * prenda contra la familia olfativa del perfume, y resta la penalización
     * si alguna nota del perfume está en la lista de exclusión.
     */
    public MatchResultDTO calculateMatchScore(List<Integer> prendasIds, Integer perfumeId) {
        Perfume perfume = perfumeRepository.findById(perfumeId)
                .orElseThrow(() -> new EntityNotFoundException("Perfume no encontrado: id " + perfumeId));

        List<Prenda> prendas = prendaRepository.findAllById(prendasIds);

        int score = PUNTUACION_BASE;
        List<String> mensajes = new ArrayList<>();
        mensajes.add("Puntuación base: " + PUNTUACION_BASE);

        // Sinergias de color y estilo, prenda por prenda
        for (Prenda prenda : prendas) {
            Optional<SinergiaColor> sinergiaColor =
                    sinergiaColorRepository.findByColorAndFamilia(prenda.getColor(), perfume.getFamilia());
            if (sinergiaColor.isPresent()) {
                int puntos = sinergiaColor.get().getPuntosSumados();
                score += puntos;
                mensajes.add(String.format("%s combina con perfume %s: %+d puntos",
                        prenda.getColor().getNombre(), perfume.getFamilia().getNombre(), puntos));
            }

            for (Estilo estilo : prenda.getEstilos()) {
                Optional<SinergiaEstilo> sinergiaEstilo =
                        sinergiaEstiloRepository.findByEstiloAndFamilia(estilo, perfume.getFamilia());
                if (sinergiaEstilo.isPresent()) {
                    int puntos = sinergiaEstilo.get().getPuntosSumados();
                    score += puntos;
                    mensajes.add(String.format("%s combina con perfume %s: %+d puntos",
                            estilo.getNombre(), perfume.getFamilia().getNombre(), puntos));
                }
            }
        }

        // Filtros de exclusión: cualquier nota bloqueada penaliza y se explica
        for (Nota nota : perfume.getNotas()) {
            Optional<FiltroExclusion> filtro = filtroExclusionRepository.findByNota(nota);
            if (filtro.isPresent()) {
                int penalizacion = filtro.get().getPenalizacionScore();
                score += penalizacion;
                mensajes.add(String.format("Nota bloqueada '%s': %+d puntos", nota.getNombre(), penalizacion));
            }
        }

        // El score final se acota entre 0 y 100
        int scoreFinal = Math.max(0, Math.min(100, score));

        return new MatchResultDTO(scoreFinal, mensajes);
    }
}
