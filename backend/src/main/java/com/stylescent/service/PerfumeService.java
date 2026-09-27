package com.stylescent.service;

import com.stylescent.dto.PerfumeRequestDTO;
import com.stylescent.dto.PerfumeResponseDTO;
import com.stylescent.model.EstadoPosesion;
import com.stylescent.model.FamiliaOlfativa;
import com.stylescent.model.Nota;
import com.stylescent.model.Perfume;
import com.stylescent.repository.EstadoPosesionRepository;
import com.stylescent.repository.FamiliaOlfativaRepository;
import com.stylescent.repository.NotaRepository;
import com.stylescent.repository.PerfumeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;
    private final FamiliaOlfativaRepository familiaRepository;
    private final EstadoPosesionRepository estadoRepository;
    private final NotaRepository notaRepository;

    public PerfumeService(PerfumeRepository perfumeRepository,
                           FamiliaOlfativaRepository familiaRepository,
                           EstadoPosesionRepository estadoRepository,
                           NotaRepository notaRepository) {
        this.perfumeRepository = perfumeRepository;
        this.familiaRepository = familiaRepository;
        this.estadoRepository = estadoRepository;
        this.notaRepository = notaRepository;
    }

    // Solo los que tiene en mano, no la lista de deseos: no tiene sentido
    // recomendar un perfume que el usuario todavía no posee.
    public List<PerfumeResponseDTO> listarEnColeccion() {
        return perfumeRepository.findByEstado_Nombre("En coleccion").stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public PerfumeResponseDTO registrar(PerfumeRequestDTO request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del perfume es obligatorio");
        }
        if (request.idFamilia() == null) {
            throw new IllegalArgumentException("Debes indicar la familia olfativa");
        }
        if (request.idEstado() == null) {
            throw new IllegalArgumentException("Debes indicar el estado de posesión");
        }

        FamiliaOlfativa familia = familiaRepository.findById(request.idFamilia())
                .orElseThrow(() -> new EntityNotFoundException("Familia olfativa no encontrada: id " + request.idFamilia()));

        EstadoPosesion estado = estadoRepository.findById(request.idEstado())
                .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado: id " + request.idEstado()));

        // Set, no List: la relación M:N con notas no admite duplicados
        Set<Nota> notas = new HashSet<>();
        if (request.idNotas() != null && !request.idNotas().isEmpty()) {
            notas.addAll(notaRepository.findAllById(request.idNotas()));
            if (notas.size() != new HashSet<>(request.idNotas()).size()) {
                throw new EntityNotFoundException("Alguna de las notas indicadas no existe");
            }
        }

        Perfume perfume = new Perfume();
        perfume.setNombre(request.nombre().trim());
        perfume.setMarca(request.marca() == null || request.marca().isBlank() ? null : request.marca().trim());
        perfume.setFamilia(familia);
        perfume.setEstado(estado);
        perfume.setNotas(notas);

        Perfume guardado = perfumeRepository.save(perfume);
        return toResponseDTO(guardado);
    }

    // Público para que el motor de puntuación devuelva el perfume con el mismo formato
    public PerfumeResponseDTO toResponseDTO(Perfume perfume) {
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
