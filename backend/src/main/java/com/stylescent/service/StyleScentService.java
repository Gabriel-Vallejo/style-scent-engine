package com.stylescent.service;

import com.stylescent.dto.DetalleMatchDTO;
import com.stylescent.dto.MatchResultDTO;
import com.stylescent.dto.RecomendacionDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

// Solo el motor de puntuación. El CRUD de prendas y perfumes vive en
// PrendaService / PerfumeService.
@Service
@Transactional(readOnly = true)
public class StyleScentService {

    private static final int PUNTUACION_BASE = 50;

    private final PrendaRepository prendaRepository;
    private final PerfumeRepository perfumeRepository;
    private final SinergiaColorRepository sinergiaColorRepository;
    private final SinergiaEstiloRepository sinergiaEstiloRepository;
    private final FiltroExclusionRepository filtroExclusionRepository;
    private final PerfumeService perfumeService;

    public StyleScentService(PrendaRepository prendaRepository,
                             PerfumeRepository perfumeRepository,
                             SinergiaColorRepository sinergiaColorRepository,
                             SinergiaEstiloRepository sinergiaEstiloRepository,
                             FiltroExclusionRepository filtroExclusionRepository,
                             PerfumeService perfumeService) {
        this.prendaRepository = prendaRepository;
        this.perfumeRepository = perfumeRepository;
        this.sinergiaColorRepository = sinergiaColorRepository;
        this.sinergiaEstiloRepository = sinergiaEstiloRepository;
        this.filtroExclusionRepository = filtroExclusionRepository;
        this.perfumeService = perfumeService;
    }

    /**
     * Calcula el Match Score (0-100) de una combinación de prendas + un perfume.
     */
    public MatchResultDTO calculateMatchScore(List<Integer> prendasIds, Integer perfumeId) {
        if (perfumeId == null) {
            throw new IllegalArgumentException("Debes indicar un perfume");
        }
        List<Prenda> prendas = cargarPrendas(prendasIds);
        Perfume perfume = perfumeRepository.findById(perfumeId)
                .orElseThrow(() -> new EntityNotFoundException("Perfume no encontrado: id " + perfumeId));

        return puntuar(prendas, perfume);
    }

    /**
     * Puntúa el outfit contra todos los perfumes en colección y los devuelve
     * ordenados de mejor a peor match.
     */
    public List<RecomendacionDTO> recomendar(List<Integer> prendasIds) {
        List<Prenda> prendas = cargarPrendas(prendasIds);

        // Mismo criterio que el listado: no se recomienda lo que aún no se tiene
        List<Perfume> enColeccion = perfumeRepository.findByEstado_Nombre("En coleccion");
        if (enColeccion.isEmpty()) {
            throw new EntityNotFoundException("No tienes perfumes en tu colección");
        }

        return enColeccion.stream()
                .map(perfume -> new RecomendacionDTO(perfumeService.toResponseDTO(perfume), puntuar(prendas, perfume)))
                .sorted(Comparator.comparingInt((RecomendacionDTO r) -> r.resultado().scoreSinAcotar()).reversed())
                .toList();
    }

    private List<Prenda> cargarPrendas(List<Integer> prendasIds) {
        if (prendasIds == null || prendasIds.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos una prenda");
        }
        List<Prenda> prendas = prendaRepository.findAllById(prendasIds);
        if (prendas.size() != new HashSet<>(prendasIds).size()) {
            throw new EntityNotFoundException("Alguna de las prendas indicadas no existe");
        }
        return prendas;
    }

    private MatchResultDTO puntuar(List<Prenda> prendas, Perfume perfume) {
        String familia = perfume.getFamilia().getNombre();

        int score = PUNTUACION_BASE;
        List<DetalleMatchDTO> detalles = new ArrayList<>();
        detalles.add(new DetalleMatchDTO("BASE", "Puntuación base", PUNTUACION_BASE));

        // Sinergias de color y estilo, prenda por prenda (List para evitar errores de unicidad)
        for (Prenda prenda : prendas) {
            List<SinergiaColor> sinergiasColor =
                    sinergiaColorRepository.findByColorAndFamilia(prenda.getColor(), perfume.getFamilia());

            for (SinergiaColor sinergia : sinergiasColor) {
                int puntos = sinergia.getPuntosSumados();
                score += puntos;
                detalles.add(new DetalleMatchDTO("COLOR", String.format("%s: color %s con %s",
                        prenda.getNombre(), prenda.getColor().getNombre(), familia), puntos));
            }

            for (Estilo estilo : prenda.getEstilos()) {
                List<SinergiaEstilo> sinergiasEstilo =
                        sinergiaEstiloRepository.findByEstiloAndFamilia(estilo, perfume.getFamilia());

                for (SinergiaEstilo sinergia : sinergiasEstilo) {
                    int puntos = sinergia.getPuntosSumados();
                    score += puntos;
                    detalles.add(new DetalleMatchDTO("ESTILO", String.format("%s: estilo %s con %s",
                            prenda.getNombre(), estilo.getNombre(), familia), puntos));
                }
            }
        }

        // Filtros de exclusión
        for (Nota nota : perfume.getNotas()) {
            Optional<FiltroExclusion> filtro = filtroExclusionRepository.findByNota(nota);
            if (filtro.isPresent()) {
                int penalizacion = filtro.get().getPenalizacionScore();
                score += penalizacion;
                detalles.add(new DetalleMatchDTO("EXCLUSION",
                        String.format("Nota bloqueada: %s", nota.getNombre()), penalizacion));
            }
        }

        int scoreFinal = Math.max(0, Math.min(100, score));
        return new MatchResultDTO(scoreFinal, score, detalles);
    }
}
