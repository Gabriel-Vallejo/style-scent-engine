package com.stylescent.dto;

import java.util.List;
import lombok.Data;

@Data
public class PrendaCreateDTO {
    private String nombre;
    private Integer categoriaId;
    private Integer colorId;
    private List<Integer> estilosIds;
}