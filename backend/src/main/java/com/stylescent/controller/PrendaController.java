package com.stylescent.controller;

import com.stylescent.dto.PrendaRequestDTO;
import com.stylescent.dto.PrendaResponseDTO;
import com.stylescent.service.PrendaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prendas")
public class PrendaController {

    private final PrendaService prendaService;

    public PrendaController(PrendaService prendaService) {
        this.prendaService = prendaService;
    }

    @GetMapping
    public List<PrendaResponseDTO> listar() {
        return prendaService.listarTodas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrendaResponseDTO registrar(@RequestBody PrendaRequestDTO request) {
        return prendaService.registrar(request);
    }
}
