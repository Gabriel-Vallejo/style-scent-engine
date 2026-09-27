package com.stylescent.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDTO {

    // Puntuación final, pensada para quedar acotada entre 0 y 100 en la lógica del service
    private int score;

    // Cada entrada explica una suma o resta concreta (ej: "Marrón + Cuero: +30")
    private List<String> mensajes;
}
