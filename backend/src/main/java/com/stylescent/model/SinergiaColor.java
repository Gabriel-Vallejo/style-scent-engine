package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sinergias_color")
@Getter
@Setter
public class SinergiaColor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Integer idRegla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_color", nullable = false)
    private Color color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_familia", nullable = false)
    private FamiliaOlfativa familia;

    @Column(name = "puntos_sumados", nullable = false)
    private Integer puntosSumados;
}
