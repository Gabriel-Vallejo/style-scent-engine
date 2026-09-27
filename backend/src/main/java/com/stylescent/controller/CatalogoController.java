package com.stylescent.controller;

import com.stylescent.dto.CatalogoItemDTO;
import com.stylescent.repository.CategoriaRepository;
import com.stylescent.repository.ColorRepository;
import com.stylescent.repository.EstadoPosesionRepository;
import com.stylescent.repository.EstiloRepository;
import com.stylescent.repository.FamiliaOlfativaRepository;
import com.stylescent.repository.NotaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints de solo lectura para que el formulario de React Native
// sepa qué IDs son válidos para categoría/color/estilo (prendas)
// y familia/nota/estado (perfumes).
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CategoriaRepository categoriaRepository;
    private final ColorRepository colorRepository;
    private final EstiloRepository estiloRepository;
    private final FamiliaOlfativaRepository familiaRepository;
    private final NotaRepository notaRepository;
    private final EstadoPosesionRepository estadoRepository;

    public CatalogoController(CategoriaRepository categoriaRepository,
                               ColorRepository colorRepository,
                               EstiloRepository estiloRepository,
                               FamiliaOlfativaRepository familiaRepository,
                               NotaRepository notaRepository,
                               EstadoPosesionRepository estadoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.colorRepository = colorRepository;
        this.estiloRepository = estiloRepository;
        this.familiaRepository = familiaRepository;
        this.notaRepository = notaRepository;
        this.estadoRepository = estadoRepository;
    }

    @GetMapping("/categorias")
    public List<CatalogoItemDTO> categorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CatalogoItemDTO(c.getIdCategoria(), c.getNombre()))
                .toList();
    }

    @GetMapping("/colores")
    public List<CatalogoItemDTO> colores() {
        return colorRepository.findAll().stream()
                .map(c -> new CatalogoItemDTO(c.getIdColor(), c.getNombre()))
                .toList();
    }

    @GetMapping("/estilos")
    public List<CatalogoItemDTO> estilos() {
        return estiloRepository.findAll().stream()
                .map(e -> new CatalogoItemDTO(e.getIdEstilo(), e.getNombre()))
                .toList();
    }

    @GetMapping("/familias")
    public List<CatalogoItemDTO> familias() {
        return familiaRepository.findAll(Sort.by("nombre")).stream()
                .map(f -> new CatalogoItemDTO(f.getIdFamilia(), f.getNombre()))
                .toList();
    }

    @GetMapping("/notas")
    public List<CatalogoItemDTO> notas() {
        return notaRepository.findAll(Sort.by("nombre")).stream()
                .map(n -> new CatalogoItemDTO(n.getIdNota(), n.getNombre()))
                .toList();
    }

    @GetMapping("/estados")
    public List<CatalogoItemDTO> estados() {
        return estadoRepository.findAll(Sort.by("idEstado")).stream()
                .map(e -> new CatalogoItemDTO(e.getIdEstado(), e.getNombre()))
                .toList();
    }
}
