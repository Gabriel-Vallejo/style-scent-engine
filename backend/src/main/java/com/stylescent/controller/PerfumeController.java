package com.stylescent.controller;

import com.stylescent.dto.PerfumeResponseDTO;
import com.stylescent.model.Nota;
import com.stylescent.model.Perfume;
import com.stylescent.repository.PerfumeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/perfumes")
public class PerfumeController {

    private final PerfumeRepository perfumeRepository;

    public PerfumeController(PerfumeRepository perfumeRepository) {
        this.perfumeRepository = perfumeRepository;
    }

    // Solo los que tiene en mano, no la lista de deseos: no tiene sentido
    // recomendar un perfume que el usuario todavía no posee.
    @GetMapping
    public List<PerfumeResponseDTO> listarEnColeccion() {
        return perfumeRepository.findByEstado_Nombre("En coleccion").stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private PerfumeResponseDTO toResponseDTO(Perfume perfume) {
        List<String> nombresNotas = perfume.getNotas().stream()
                .map(Nota::getNombre)
                .toList();

        return new PerfumeResponseDTO(
                perfume.getIdPerfume(),
                perfume.getNombre(),
                perfume.getMarca(),
                perfume.getFamilia().getNombre(),
                nombresNotas,
                perfume.getEstado().getNombre()
        );
    }
}
