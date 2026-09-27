package com.stylescent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "colores")
@Getter
@Setter
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_color")
    private Integer idColor;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;
}
