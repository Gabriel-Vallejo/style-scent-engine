package com.stylescent.service;

import com.stylescent.dto.MatchResultDTO;
import com.stylescent.model.Color;
import com.stylescent.model.Estilo;
import com.stylescent.model.FamiliaOlfativa;
import com.stylescent.model.FiltroExclusion;
import com.stylescent.model.Nota;
import com.stylescent.model.Perfume;
import com.stylescent.model.Prenda;
import com.stylescent.model.SinergiaColor;
import com.stylescent.model.SinergiaEstilo;
import com.stylescent.repository.FiltroExclusionRepository;
import com.stylescent.repository.PerfumeRepository;
import com.stylescent.repository.PrendaRepository;
import com.stylescent.repository.SinergiaColorRepository;
import com.stylescent.repository.SinergiaEstiloRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StyleScentServiceTest {

    @Mock private PrendaRepository prendaRepository;
    @Mock private PerfumeRepository perfumeRepository;
    @Mock private SinergiaColorRepository sinergiaColorRepository;
    @Mock private SinergiaEstiloRepository sinergiaEstiloRepository;
    @Mock private FiltroExclusionRepository filtroExclusionRepository;

    @InjectMocks
    private StyleScentService service;

    private FamiliaOlfativa cuero;
    private Color marron;
    private Estilo streetwear;
    private Perfume perfume;
    private Prenda chaqueta;

    @BeforeEach
    void setUp() {
        cuero = new FamiliaOlfativa();
        cuero.setIdFamilia(1);
        cuero.setNombre("Cuero");

        marron = new Color();
        marron.setIdColor(1);
        marron.setNombre("Marrón");

        streetwear = new Estilo();
        streetwear.setIdEstilo(1);
        streetwear.setNombre("Streetwear");

        perfume = new Perfume();
        perfume.setIdPerfume(17);
        perfume.setNombre("Tom Ford Ombré Leather");
        perfume.setFamilia(cuero);
        perfume.setNotas(new HashSet<>());

        chaqueta = new Prenda();
        chaqueta.setIdPrenda(1);
        chaqueta.setNombre("Chaqueta Leon S. Kennedy");
        chaqueta.setColor(marron);
        Set<Estilo> estilos = new HashSet<>();
        estilos.add(streetwear);
        chaqueta.setEstilos(estilos);
    }

    @Test
    void sumaPuntosDeSinergiaDeColor() {
        SinergiaColor sinergia = new SinergiaColor();
        sinergia.setPuntosSumados(30);

        when(perfumeRepository.findById(17)).thenReturn(Optional.of(perfume));
        when(prendaRepository.findAllById(List.of(1))).thenReturn(List.of(chaqueta));
        when(sinergiaColorRepository.findByColorAndFamilia(marron, cuero)).thenReturn(Optional.of(sinergia));
        when(sinergiaEstiloRepository.findByEstiloAndFamilia(streetwear, cuero)).thenReturn(Optional.empty());

        MatchResultDTO resultado = service.calculateMatchScore(List.of(1), 17);

        assertThat(resultado.getScore()).isEqualTo(80); // 50 base + 30
        assertThat(resultado.getMensajes()).anyMatch(m -> m.contains("Marrón"));
    }

    @Test
    void sumaPuntosDeSinergiaDeEstilo() {
        SinergiaEstilo sinergia = new SinergiaEstilo();
        sinergia.setPuntosSumados(20);

        when(perfumeRepository.findById(17)).thenReturn(Optional.of(perfume));
        when(prendaRepository.findAllById(List.of(1))).thenReturn(List.of(chaqueta));
        when(sinergiaColorRepository.findByColorAndFamilia(marron, cuero)).thenReturn(Optional.empty());
        when(sinergiaEstiloRepository.findByEstiloAndFamilia(streetwear, cuero)).thenReturn(Optional.of(sinergia));

        MatchResultDTO resultado = service.calculateMatchScore(List.of(1), 17);

        assertThat(resultado.getScore()).isEqualTo(70); // 50 base + 20
    }

    @Test
    void aplicaPenalizacionPorNotaBloqueada() {
        Nota iris = new Nota();
        iris.setIdNota(5);
        iris.setNombre("Iris");
        perfume.setNotas(Set.of(iris));

        FiltroExclusion filtro = new FiltroExclusion();
        filtro.setPenalizacionScore(-100);

        when(perfumeRepository.findById(17)).thenReturn(Optional.of(perfume));
        when(prendaRepository.findAllById(List.of(1))).thenReturn(List.of(chaqueta));
        when(sinergiaColorRepository.findByColorAndFamilia(marron, cuero)).thenReturn(Optional.empty());
        when(sinergiaEstiloRepository.findByEstiloAndFamilia(streetwear, cuero)).thenReturn(Optional.empty());
        when(filtroExclusionRepository.findByNota(iris)).thenReturn(Optional.of(filtro));

        MatchResultDTO resultado = service.calculateMatchScore(List.of(1), 17);

        assertThat(resultado.getScore()).isEqualTo(0); // 50 - 100, acotado a 0
        assertThat(resultado.getMensajes()).anyMatch(m -> m.contains("Iris"));
    }

    @Test
    void acotaElScoreA100ComoMaximo() {
        SinergiaColor sinergiaColor = new SinergiaColor();
        sinergiaColor.setPuntosSumados(80);

        when(perfumeRepository.findById(17)).thenReturn(Optional.of(perfume));
        when(prendaRepository.findAllById(List.of(1))).thenReturn(List.of(chaqueta));
        when(sinergiaColorRepository.findByColorAndFamilia(marron, cuero)).thenReturn(Optional.of(sinergiaColor));
        when(sinergiaEstiloRepository.findByEstiloAndFamilia(streetwear, cuero)).thenReturn(Optional.empty());

        MatchResultDTO resultado = service.calculateMatchScore(List.of(1), 17);

        assertThat(resultado.getScore()).isEqualTo(100); // 50 + 80 = 130, acotado a 100
    }

    @Test
    void lanzaExcepcionSiElPerfumeNoExiste() {
        when(perfumeRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculateMatchScore(List.of(1), 999))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
