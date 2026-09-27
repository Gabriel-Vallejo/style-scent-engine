package com.stylescent.controller;

import com.stylescent.dto.MatchResultDTO;
import com.stylescent.dto.PrendaCreateDTO;
import com.stylescent.model.Categoria;
import com.stylescent.model.Color;
import com.stylescent.model.Estilo;
import com.stylescent.model.Prenda;
import com.stylescent.service.StyleScentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prendas")
public class PrendaController {

    private final StyleScentService styleScentService;

    public PrendaController(StyleScentService styleScentService) {
        this.styleScentService = styleScentService;
    }

    @PostMapping
    public ResponseEntity<?> crearPrenda(@RequestBody PrendaCreateDTO dto) {
        try {
            styleScentService.registrarPrenda(dto);
            return ResponseEntity.ok().body("{\"mensaje\": \"Prenda registrada con éxito en el Armario Digital\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // --- NUEVO ENDPOINT PARA EL ARMARIO COMPLETO ---
    @GetMapping
    public ResponseEntity<List<Prenda>> getTodasLasPrendas() {
        return ResponseEntity.ok(styleScentService.obtenerTodasLasPrendas());
    }

    // --- ENDPOINTS GET DE CATÁLOGOS ---

    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> getCategorias() {
        return ResponseEntity.ok(styleScentService.obtenerCategorias());
    }

    @GetMapping("/colores")
    public ResponseEntity<List<Color>> getColores() {
        return ResponseEntity.ok(styleScentService.obtenerColores());
    }

    @GetMapping("/estilos")
    public ResponseEntity<List<Estilo>> getEstilos() {
        return ResponseEntity.ok(styleScentService.obtenerEstilos());
    }
    // --- NUEVO ENDPOINT PARA EL ALGORITMO DE MATCH ---
    @PostMapping("/match")
    public ResponseEntity<?> calcularRecomendacion(@RequestBody List<Integer> prendasIds) {
        try {
            MatchResultDTO resultado = styleScentService.recomendarMejorPerfume(prendasIds);

            // Devolvemos el nombre del mejor perfume y los mensajes del desglose si los quieres usar
            return ResponseEntity.ok().body("{\"perfumeRecomendado\": \"" + resultado.getMensajes().get(0) + "\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}