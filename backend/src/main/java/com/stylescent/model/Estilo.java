package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estilos")
@Getter
@Setter
public class Estilo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estilo")
    private Integer idEstilo;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;
}
