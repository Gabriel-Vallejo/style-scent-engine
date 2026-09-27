package com.stylescent.controller;

import com.stylescent.dto.CatalogoItemDTO;
import com.stylescent.repository.CategoriaRepository;
import com.stylescent.repository.ColorRepository;
import com.stylescent.repository.EstiloRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints de solo lectura para que el formulario de React Native
// sepa qué IDs son válidos para categoría/color/estilo.
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CategoriaRepository categoriaRepository;
    private final ColorRepository colorRepository;
    private final EstiloRepository estiloRepository;

    public CatalogoController(CategoriaRepository categoriaRepository,
                               ColorRepository colorRepository,
                               EstiloRepository estiloRepository) {
        this.categoriaRepository = categoriaRepository;
        this.colorRepository = colorRepository;
        this.estiloRepository = estiloRepository;
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
}
