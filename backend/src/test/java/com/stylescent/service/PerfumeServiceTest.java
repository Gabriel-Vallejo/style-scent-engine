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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfumeServiceTest {

    @Mock private PerfumeRepository perfumeRepository;
    @Mock private FamiliaOlfativaRepository familiaRepository;
    @Mock private EstadoPosesionRepository estadoRepository;
    @Mock private NotaRepository notaRepository;

    @InjectMocks
    private PerfumeService service;

    private FamiliaOlfativa cuero;
    private EstadoPosesion enColeccion;
    private Nota cueroNota;
    private Nota azafran;

    @BeforeEach
    void setUp() {
        cuero = new FamiliaOlfativa();
        cuero.setIdFamilia(10);
        cuero.setNombre("Cuero");

        enColeccion = new EstadoPosesion();
        enColeccion.setIdEstado(1);
        enColeccion.setNombre("En coleccion");

        cueroNota = new Nota();
        cueroNota.setIdNota(28);
        cueroNota.setNombre("Cuero");

        azafran = new Nota();
        azafran.setIdNota(31);
        azafran.setNombre("Azafrán");
    }

    @Test
    void registrar_guardaPerfumeConFamiliaEstadoYNotas() {
        when(familiaRepository.findById(10)).thenReturn(Optional.of(cuero));
        when(estadoRepository.findById(1)).thenReturn(Optional.of(enColeccion));
        when(notaRepository.findAllById(List.of(28, 31))).thenReturn(List.of(cueroNota, azafran));
        when(perfumeRepository.save(any(Perfume.class))).thenAnswer(inv -> {
            Perfume p = inv.getArgument(0);
            p.setIdPerfume(22);
            return p;
        });

        PerfumeResponseDTO resultado = service.registrar(
                new PerfumeRequestDTO("  Ombré Leather ", "Tom Ford", 10, 1, List.of(28, 31)));

        assertThat(resultado.idPerfume()).isEqualTo(22);
        assertThat(resultado.nombre()).isEqualTo("Ombré Leather");
        assertThat(resultado.marca()).isEqualTo("Tom Ford");
        assertThat(resultado.familia()).isEqualTo("Cuero");
        assertThat(resultado.estado()).isEqualTo("En coleccion");
        assertThat(resultado.notas()).containsExactlyInAnyOrder("Cuero", "Azafrán");
    }

    @Test
    void registrar_sinNotasYMarcaVacia_esValido() {
        when(familiaRepository.findById(10)).thenReturn(Optional.of(cuero));
        when(estadoRepository.findById(1)).thenReturn(Optional.of(enColeccion));
        when(perfumeRepository.save(any(Perfume.class))).thenAnswer(inv -> inv.getArgument(0));

        PerfumeResponseDTO resultado = service.registrar(
                new PerfumeRequestDTO("Perfume casero", "  ", 10, 1, List.of()));

        assertThat(resultado.marca()).isNull();
        assertThat(resultado.notas()).isEmpty();
    }

    @Test
    void registrar_sinNombre_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.registrar(new PerfumeRequestDTO(" ", "X", 10, 1, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(perfumeRepository, never()).save(any());
    }

    @Test
    void registrar_familiaInexistente_lanzaEntityNotFound() {
        when(familiaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(new PerfumeRequestDTO("X", null, 99, 1, List.of())))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
        verify(perfumeRepository, never()).save(any());
    }

    @Test
    void registrar_notaInexistente_lanzaEntityNotFound() {
        when(familiaRepository.findById(10)).thenReturn(Optional.of(cuero));
        when(estadoRepository.findById(1)).thenReturn(Optional.of(enColeccion));
        when(notaRepository.findAllById(List.of(28, 999))).thenReturn(List.of(cueroNota));

        assertThatThrownBy(() -> service.registrar(new PerfumeRequestDTO("X", null, 10, 1, List.of(28, 999))))
                .isInstanceOf(EntityNotFoundException.class);
        verify(perfumeRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_actualizaElEstadoDelPerfume() {
        EstadoPosesion enCamino = new EstadoPosesion();
        enCamino.setIdEstado(2);
        enCamino.setNombre("En camino");

        Perfume perfume = new Perfume();
        perfume.setIdPerfume(5);
        perfume.setNombre("Vulcan Feu");
        perfume.setFamilia(cuero);
        perfume.setEstado(enCamino);

        when(perfumeRepository.findById(5)).thenReturn(Optional.of(perfume));
        when(estadoRepository.findById(1)).thenReturn(Optional.of(enColeccion));

        PerfumeResponseDTO resultado = service.cambiarEstado(5, 1);

        assertThat(resultado.estado()).isEqualTo("En coleccion");
        assertThat(perfume.getEstado()).isSameAs(enColeccion);
    }

    @Test
    void cambiarEstado_estadoInexistente_lanzaEntityNotFound() {
        Perfume perfume = new Perfume();
        when(perfumeRepository.findById(5)).thenReturn(Optional.of(perfume));
        when(estadoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(5, 99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminar_perfumeInexistente_lanzaEntityNotFound() {
        when(perfumeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99))
                .isInstanceOf(EntityNotFoundException.class);
        verify(perfumeRepository, never()).delete(any());
    }
}
