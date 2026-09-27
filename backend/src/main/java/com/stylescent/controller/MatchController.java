package com.stylescent.controller;

import com.stylescent.dto.MatchResultDTO;
import com.stylescent.service.StyleScentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final StyleScentService styleScentService;

    public MatchController(StyleScentService styleScentService) {
        this.styleScentService = styleScentService;
    }

    @PostMapping
    public MatchResultDTO calculateMatch(@RequestBody MatchRequest request) {
        return styleScentService.calculateMatchScore(request.prendasIds(), request.perfumeId());
    }

    // Record como cuerpo de la petición: { "prendasIds": [1, 2, 3], "perfumeId": 17 }
    public record MatchRequest(List<Integer> prendasIds, Integer perfumeId) {
    }
}
