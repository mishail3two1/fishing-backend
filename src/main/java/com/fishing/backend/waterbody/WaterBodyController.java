package com.fishing.backend.waterbody;

import com.fishing.backend.waterbody.dto.WaterBodyMapItem;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/water-bodies")
public class WaterBodyController {

    private final WaterBodyService service;

    public WaterBodyController(WaterBodyService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Опубликованные водоёмы для карты")
    public List<WaterBodyMapItem> list() {
        return service.findPublished();
    }
}