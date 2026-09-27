package com.stylescent.repository;

import com.stylescent.model.Color;
import com.stylescent.model.FamiliaOlfativa;
import com.stylescent.model.SinergiaColor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SinergiaColorRepository extends JpaRepository<SinergiaColor, Integer> {

    // Todas las reglas para un color (útil si quieres ver el desglose completo)
    List<SinergiaColor> findByColor(Color color);

    // La regla exacta color + familia, la que usa el motor de puntuación
    Optional<SinergiaColor> findByColorAndFamilia(Color color, FamiliaOlfativa familia);
}
