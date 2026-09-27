package com.stylescent.repository;

import com.stylescent.model.Estilo;
import com.stylescent.model.FamiliaOlfativa;
import com.stylescent.model.SinergiaEstilo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SinergiaEstiloRepository extends JpaRepository<SinergiaEstilo, Integer> {

    List<SinergiaEstilo> findByEstilo(Estilo estilo);

    // Cambiado a List para admitir múltiples reglas de estilo sin romper el motor
    List<SinergiaEstilo> findByEstiloAndFamilia(Estilo estilo, FamiliaOlfativa familia);
}