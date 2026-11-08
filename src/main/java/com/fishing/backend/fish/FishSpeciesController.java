package com.fishing.backend.fish;

import com.fishing.backend.fish.dto.FishSpeciesItem;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fish-species")
public class FishSpeciesController {

    private final FishSpeciesService service;

    public FishSpeciesController(FishSpeciesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Справочник видов рыб (по алфавиту)")
    public List<FishSpeciesItem> list() {
        return service.findAll();
    }
}
