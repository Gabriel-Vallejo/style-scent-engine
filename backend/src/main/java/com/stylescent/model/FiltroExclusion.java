package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "filtros_exclusion")
@Getter
@Setter
public class FiltroExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_filtro")
    private Integer idFiltro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nota", nullable = false)
    private Nota nota;

    @Column(name = "penalizacion_score", nullable = false)
    private Integer penalizacionScore;
}
