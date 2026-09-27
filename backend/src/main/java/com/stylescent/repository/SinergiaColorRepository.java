package com.stylescent.repository;

import com.stylescent.model.Color;
import com.stylescent.model.FamiliaOlfativa;
import com.stylescent.model.SinergiaColor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SinergiaColorRepository extends JpaRepository<SinergiaColor, Integer> {

    // Todas las reglas para un color
    List<SinergiaColor> findByColor(Color color);

    // List por compatibilidad; con UNIQUE (id_color, id_familia) devuelve 0 o 1 regla
    List<SinergiaColor> findByColorAndFamilia(Color color, FamiliaOlfativa familia);
}