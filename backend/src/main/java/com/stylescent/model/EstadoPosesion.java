package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estados_posesion")
@Getter
@Setter
public class EstadoPosesion {

    // Único estado que cuenta para recomendar: lo que el usuario ya tiene.
    // Debe coincidir exactamente (tildes incluidas) con la fila de estados_posesion.
    public static final String EN_COLECCION = "En colección";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Integer idEstado;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;
}
