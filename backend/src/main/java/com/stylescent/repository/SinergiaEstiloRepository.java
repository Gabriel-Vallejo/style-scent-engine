package com.stylescent.repository;

import com.stylescent.model.Estilo;
import com.stylescent.model.FamiliaOlfativa;
import com.stylescent.model.SinergiaEstilo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SinergiaEstiloRepository extends JpaRepository<SinergiaEstilo, Integer> {

    List<SinergiaEstilo> findByEstilo(Estilo estilo);

    Optional<SinergiaEstilo> findByEstiloAndFamilia(Estilo estilo, FamiliaOlfativa familia);
}
