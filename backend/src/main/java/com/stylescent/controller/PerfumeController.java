package com.stylescent.controller;

import com.stylescent.dto.CambioEstadoRequestDTO;
import com.stylescent.dto.PerfumeRequestDTO;
import com.stylescent.dto.PerfumeResponseDTO;
import com.stylescent.service.PerfumeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/perfumes")
public class PerfumeController {

    private final PerfumeService perfumeService;

    public PerfumeController(PerfumeService perfumeService) {
        this.perfumeService = perfumeService;
    }

    // Por defecto solo los que están en colección (lo que usa Recomendar);
    // ?todos=true para la pantalla de gestión.
    @GetMapping
    public List<PerfumeResponseDTO> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return todos ? perfumeService.listarTodos() : perfumeService.listarEnColeccion();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerfumeResponseDTO registrar(@RequestBody PerfumeRequestDTO request) {
        return perfumeService.registrar(request);
    }

    @PatchMapping("/{id}/estado")
    public PerfumeResponseDTO cambiarEstado(@PathVariable Integer id, @RequestBody CambioEstadoRequestDTO request) {
        return perfumeService.cambiarEstado(id, request.idEstado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        perfumeService.eliminar(id);
    }
}
