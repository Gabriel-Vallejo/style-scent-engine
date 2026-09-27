package com.stylescent.service;

import com.stylescent.dto.AnalisisPrendaDTO;
import com.stylescent.dto.SugerenciaDTO;
import com.stylescent.model.Categoria;
import com.stylescent.model.Color;
import com.stylescent.model.Estilo;
import com.stylescent.repository.CategoriaRepository;
import com.stylescent.repository.ColorRepository;
import com.stylescent.repository.EstiloRepository;
import com.stylescent.vision.AnalisisVision;
import com.stylescent.vision.AnalisisVision.Sugerencia;
import com.stylescent.vision.VisionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalisisPrendaServiceTest {

    @Mock private VisionClient visionClient;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ColorRepository colorRepository;
    @Mock private EstiloRepository estiloRepository;

    @InjectMocks
    private AnalisisPrendaService service;

    private static final byte[] FOTO = {1, 2, 3};

    @BeforeEach
    void setUp() {
        Categoria torso = new Categoria();
        torso.setIdCategoria(1);
        torso.setNombre("Torso");
        Categoria calzado = new Categoria();
        calzado.setIdCategoria(3);
        calzado.setNombre("Calzado");

        Color marron = new Color();
        marron.setIdColor(1);
        marron.setNombre("Marrón");
        Color negro = new Color();
        negro.setIdColor(2);
        negro.setNombre("Negro");

        Estilo streetwear = new Estilo();
        streetwear.setIdEstilo(1);
        streetwear.setNombre("Streetwear");
        Estilo casual = new Estilo();
        casual.setIdEstilo(3);
        casual.setNombre("Casual");

        when(categoriaRepository.findAll()).thenReturn(List.of(torso, calzado));
        when(colorRepository.findAll()).thenReturn(List.of(marron, negro));
        when(estiloRepository.findAll()).thenReturn(List.of(streetwear, casual));
    }

    @Test
    void traduceLasSugerenciasDeVisionAIdsDelCatalogo() {
        when(visionClient.analizar(eq(FOTO), eq("chaqueta.jpg"), anyList(), anyList(), anyList()))
                .thenReturn(new AnalisisVision(
                        List.of(new Sugerencia("Torso", 0.96), new Sugerencia("Calzado", 0.04)),
                        List.of(new Sugerencia("Marrón", 0.8), new Sugerencia("Negro", 0.2)),
                        List.of(new Sugerencia("Streetwear", 0.6), new Sugerencia("Casual", 0.4)),
                        List.of("Streetwear", "Casual"),
                        "#644428",
                        "kmeans"));

        AnalisisPrendaDTO resultado = service.analizar(FOTO, "chaqueta.jpg");

        assertThat(resultado.categoria()).isEqualTo(new SugerenciaDTO(1, "Torso", 0.96));
        assertThat(resultado.color()).isEqualTo(new SugerenciaDTO(1, "Marrón", 0.8));
        assertThat(resultado.estilos()).containsExactly(
                new SugerenciaDTO(1, "Streetwear", 0.6),
                new SugerenciaDTO(3, "Casual", 0.4));
        assertThat(resultado.colorDominanteHex()).isEqualTo("#644428");
    }

    @Test
    void mandaAlServicioDeVisionLosNombresDelCatalogo() {
        when(visionClient.analizar(any(), any(), anyList(), anyList(), anyList())).thenAnswer(inv -> {
            assertThat((List<String>) inv.getArgument(2)).containsExactlyInAnyOrder("Torso", "Calzado");
            assertThat((List<String>) inv.getArgument(3)).containsExactlyInAnyOrder("Marrón", "Negro");
            assertThat((List<String>) inv.getArgument(4)).containsExactlyInAnyOrder("Streetwear", "Casual");
            return new AnalisisVision(List.of(), List.of(), List.of(), List.of(), "#000000", "kmeans");
        });

        AnalisisPrendaDTO resultado = service.analizar(FOTO, "x.jpg");

        // Sin sugerencias válidas no se inventa nada: el usuario elige a mano
        assertThat(resultado.categoria()).isNull();
        assertThat(resultado.color()).isNull();
        assertThat(resultado.estilos()).isEmpty();
    }

    @Test
    void ignoraNombresQueYaNoExistenEnElCatalogo() {
        when(visionClient.analizar(any(), any(), anyList(), anyList(), anyList()))
                .thenReturn(new AnalisisVision(
                        List.of(new Sugerencia("Renombrada", 0.9), new Sugerencia("Calzado", 0.1)),
                        List.of(new Sugerencia("Negro", 1.0)),
                        List.of(new Sugerencia("Streetwear", 1.0)),
                        List.of("Borrado", "Streetwear"),
                        "#111111",
                        "kmeans"));

        AnalisisPrendaDTO resultado = service.analizar(FOTO, "x.jpg");

        assertThat(resultado.categoria().nombre()).isEqualTo("Calzado");
        assertThat(resultado.estilos()).extracting(SugerenciaDTO::nombre).containsExactly("Streetwear");
    }
}

class AnalisisPrendaServiceSinFotoTest {

    @Test
    void sinFotoLanzaIllegalArgumentSinLlamarAVision() {
        VisionClient visionClient = org.mockito.Mockito.mock(VisionClient.class);
        AnalisisPrendaService service = new AnalisisPrendaService(visionClient,
                org.mockito.Mockito.mock(CategoriaRepository.class),
                org.mockito.Mockito.mock(ColorRepository.class),
                org.mockito.Mockito.mock(EstiloRepository.class));

        assertThatThrownBy(() -> service.analizar(new byte[0], "vacia.jpg"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(visionClient);
    }
}
