package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "familias_olfativas")
@Getter
@Setter
public class FamiliaOlfativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_familia")
    private Integer idFamilia;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;
}
