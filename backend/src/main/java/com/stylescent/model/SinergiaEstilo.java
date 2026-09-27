package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sinergias_estilo")
@Getter
@Setter
public class SinergiaEstilo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Integer idRegla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estilo", nullable = false)
    private Estilo estilo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_familia", nullable = false)
    private FamiliaOlfativa familia;

    @Column(name = "puntos_sumados", nullable = false)
    private Integer puntosSumados;
}
