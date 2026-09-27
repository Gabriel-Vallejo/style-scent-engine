package com.stylescent.controller;

import com.stylescent.dto.PerfumeRequestDTO;
import com.stylescent.dto.PerfumeResponseDTO;
import com.stylescent.service.PerfumeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping
    public List<PerfumeResponseDTO> listarEnColeccion() {
        return perfumeService.listarEnColeccion();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerfumeResponseDTO registrar(@RequestBody PerfumeRequestDTO request) {
        return perfumeService.registrar(request);
    }
}
