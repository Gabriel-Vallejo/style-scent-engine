package com.stylescent.repository;

import com.stylescent.model.FiltroExclusion;
import com.stylescent.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FiltroExclusionRepository extends JpaRepository<FiltroExclusion, Integer> {

    // Comprobar si una nota concreta tiene una penalización configurada
    Optional<FiltroExclusion> findByNota(Nota nota);

    boolean existsByNota(Nota nota);
}
