package com.stylescent.repository;

import com.stylescent.model.Color;
import com.stylescent.model.FamiliaOlfativa;
import com.stylescent.model.SinergiaColor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SinergiaColorRepository extends JpaRepository<SinergiaColor, Integer> {

    // Todas las reglas para un color
    List<SinergiaColor> findByColor(Color color);

    // Cambiado a List para admitir múltiples reglas sin romper el motor
    List<SinergiaColor> findByColorAndFamilia(Color color, FamiliaOlfativa familia);
}