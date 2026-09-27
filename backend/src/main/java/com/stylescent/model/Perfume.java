package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "perfumes")
@Getter
@Setter
public class Perfume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfume")
    private Integer idPerfume;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "marca", length = 50)
    private String marca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_familia", nullable = false)
    private FamiliaOlfativa familia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoPosesion estado;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "perfume_nota",
        joinColumns = @JoinColumn(name = "id_perfume"),
        inverseJoinColumns = @JoinColumn(name = "id_nota")
    )
    private Set<Nota> notas = new HashSet<>();
}
