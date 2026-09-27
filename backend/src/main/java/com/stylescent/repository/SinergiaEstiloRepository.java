package com.stylescent.repository;

import com.stylescent.model.Estilo;
import com.stylescent.model.FamiliaOlfativa;
import com.stylescent.model.SinergiaEstilo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SinergiaEstiloRepository extends JpaRepository<SinergiaEstilo, Integer> {

    List<SinergiaEstilo> findByEstilo(Estilo estilo);

    // List por compatibilidad; con UNIQUE (id_estilo, id_familia) devuelve 0 o 1 regla
    List<SinergiaEstilo> findByEstiloAndFamilia(Estilo estilo, FamiliaOlfativa familia);
}