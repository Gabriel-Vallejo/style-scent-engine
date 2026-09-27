package com.stylescent.service;

import com.stylescent.dto.PrendaRequestDTO;
import com.stylescent.dto.PrendaResponseDTO;
import com.stylescent.model.Categoria;
import com.stylescent.model.Color;
import com.stylescent.model.Estilo;
import com.stylescent.model.Prenda;
import com.stylescent.repository.CategoriaRepository;
import com.stylescent.repository.ColorRepository;
import com.stylescent.repository.EstiloRepository;
import com.stylescent.repository.PrendaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PrendaService {

    private final PrendaRepository prendaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ColorRepository colorRepository;
    private final EstiloRepository estiloRepository;

    public PrendaService(PrendaRepository prendaRepository,
                          CategoriaRepository categoriaRepository,
                          ColorRepository colorRepository,
                          EstiloRepository estiloRepository) {
        this.prendaRepository = prendaRepository;
        this.categoriaRepository = categoriaRepository;
        this.colorRepository = colorRepository;
        this.estiloRepository = estiloRepository;
    }

    public List<PrendaResponseDTO> listarTodas() {
        return prendaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public PrendaResponseDTO registrar(PrendaRequestDTO request) {
        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: id " + request.idCategoria()));

        Color color = colorRepository.findById(request.idColor())
                .orElseThrow(() -> new EntityNotFoundException("Color no encontrado: id " + request.idColor()));

        if (request.idEstilos() == null || request.idEstilos().isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos un estilo");
        }

        // Set, no List: la relación M:N con estilos no admite duplicados
        Set<Estilo> estilos = new HashSet<>(estiloRepository.findAllById(request.idEstilos()));
        if (estilos.size() != new HashSet<>(request.idEstilos()).size()) {
            throw new EntityNotFoundException("Alguno de los estilos indicados no existe");
        }

        Prenda prenda = new Prenda();
        prenda.setNombre(request.nombre());
        prenda.setCategoria(categoria);
        prenda.setColor(color);
        prenda.setEstilos(estilos);
        prenda.setFechaRegistro(LocalDateTime.now());

        Prenda guardada = prendaRepository.save(prenda);
        return toResponseDTO(guardada);
    }

    private PrendaResponseDTO toResponseDTO(Prenda prenda) {
        List<String> nombresEstilos = prenda.getEstilos().stream()
                .map(Estilo::getNombre)
                .toList();

        return new PrendaResponseDTO(
                prenda.getIdPrenda(),
                prenda.getNombre(),
                prenda.getCategoria().getNombre(),
                prenda.getColor().getNombre(),
                nombresEstilos,
                prenda.getFechaRegistro().toString()
        );
    }
}
