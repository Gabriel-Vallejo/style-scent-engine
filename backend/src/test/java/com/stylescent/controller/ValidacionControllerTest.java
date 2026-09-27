package com.stylescent.controller;

import com.stylescent.service.AnalisisPrendaService;
import com.stylescent.service.PerfumeService;
import com.stylescent.service.PrendaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Capa web sin BD: validación de los DTOs y formato uniforme de los errores
@WebMvcTest({PrendaController.class, PerfumeController.class})
class ValidacionControllerTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private PrendaService prendaService;
    @MockitoBean private AnalisisPrendaService analisisPrendaService;
    @MockitoBean private PerfumeService perfumeService;

    @Test
    void prendaConNombreVacioDevuelve400YNoSeGuarda() throws Exception {
        mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  \",\"idCategoria\":1,\"idColor\":1,\"idEstilos\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre de la prenda es obligatorio"));
        verify(prendaService, never()).registrar(any());
    }

    @Test
    void prendaConNombreDemasiadoLargoDevuelve400() throws Exception {
        String nombre = "a".repeat(101);
        mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"idCategoria\":1,\"idColor\":1,\"idEstilos\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("100 caracteres")));
    }

    @Test
    void prendaSinCategoriaNiEstilosJuntaLosMensajes() throws Exception {
        mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Chaqueta\",\"idColor\":1,\"idEstilos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("categoría")))
                .andExpect(jsonPath("$.message").value(containsString("estilo")));
    }

    @Test
    void perfumeConMarcaDemasiadoLargaDevuelve400() throws Exception {
        String marca = "b".repeat(51);
        mvc.perform(post("/api/perfumes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"marca\":\"" + marca + "\",\"idFamilia\":1,\"idEstado\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La marca no puede superar los 50 caracteres"));
        verify(perfumeService, never()).registrar(any());
    }

    @Test
    void jsonMalFormadoDevuelve400ConMensaje() throws Exception {
        mvc.perform(post("/api/prendas").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la petición no es un JSON válido"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void idNoNumericoEnLaRutaDevuelve400ConMensaje() throws Exception {
        mvc.perform(delete("/api/perfumes/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'id' no tiene un formato válido"));
    }

    @Test
    void analizarSinImagenDevuelve400ConMensaje() throws Exception {
        mvc.perform(multipart("/api/prendas/analizar").param("otra", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el campo 'imagen' en la petición"));
    }
}
