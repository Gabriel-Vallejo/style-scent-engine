package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notas")
@Getter
@Setter
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota")
    private Integer idNota;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;
}
