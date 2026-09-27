package com.stylescent.service;

import com.stylescent.model.Prenda;
import com.stylescent.repository.CategoriaRepository;
import com.stylescent.repository.ColorRepository;
import com.stylescent.repository.EstiloRepository;
import com.stylescent.repository.PrendaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrendaServiceTest {

    @Mock private PrendaRepository prendaRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ColorRepository colorRepository;
    @Mock private EstiloRepository estiloRepository;

    @InjectMocks
    private PrendaService service;

    @Test
    void eliminar_borraLaPrendaExistente() {
        Prenda prenda = new Prenda();
        prenda.setIdPrenda(5);
        when(prendaRepository.findById(5)).thenReturn(Optional.of(prenda));

        service.eliminar(5);

        verify(prendaRepository).delete(prenda);
    }

    @Test
    void eliminar_prendaInexistente_lanzaEntityNotFound() {
        when(prendaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99))
                .isInstanceOf(EntityNotFoundException.class);
        verify(prendaRepository, never()).delete(any());
    }
}
