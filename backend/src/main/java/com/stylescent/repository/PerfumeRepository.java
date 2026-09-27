package com.stylescent.repository;

import com.stylescent.model.EstadoPosesion;
import com.stylescent.model.Perfume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerfumeRepository extends JpaRepository<Perfume, Integer> {

    // Filtrar pasando la entidad EstadoPosesion ya cargada
    List<Perfume> findByEstado(EstadoPosesion estado);

    // Filtrar directamente con el texto, sin cargar antes la entidad
    // (ej: findByEstado_Nombre("En colección") / findByEstado_Nombre("Lista de deseos"))
    List<Perfume> findByEstado_Nombre(String nombre);
}
