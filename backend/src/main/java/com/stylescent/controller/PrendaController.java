package com.stylescent.controller;

import com.stylescent.dto.AnalisisPrendaDTO;
import com.stylescent.dto.PrendaRequestDTO;
import com.stylescent.dto.PrendaResponseDTO;
import com.stylescent.service.AnalisisPrendaService;
import com.stylescent.service.PrendaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;

@RestController
@RequestMapping("/api/prendas")
public class PrendaController {

    private final PrendaService prendaService;
    private final AnalisisPrendaService analisisPrendaService;

    public PrendaController(PrendaService prendaService, AnalisisPrendaService analisisPrendaService) {
        this.prendaService = prendaService;
        this.analisisPrendaService = analisisPrendaService;
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        prendaService.eliminar(id);
    }

    // Fase 2: sugiere categoría, color y estilos a partir de una foto (no guarda nada)
    @PostMapping(value = "/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalisisPrendaDTO analizar(@RequestPart("imagen") MultipartFile imagen) throws IOException {
        return analisisPrendaService.analizar(imagen.getBytes(), imagen.getOriginalFilename());
    }
}
