package com.fishing.backend.waterbody;

import com.fishing.backend.waterbody.dto.WaterBodyCard;
import com.fishing.backend.waterbody.dto.WaterBodyMapItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/water-bodies")
public class WaterBodyController {

    private final WaterBodyService service;

    public WaterBodyController(WaterBodyService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Опубликованные водоёмы в видимой области карты")
    public List<WaterBodyMapItem> list(
            @Parameter(description = "Область карты: minLon,minLat,maxLon,maxLat (градусы WGS 84)",
                    example = "61.0,54.9,61.8,55.4")
            @RequestParam List<Double> bbox) {

        // пустой элемент ("61.0,,61.8,55.4") Spring превращает в null
        if (bbox.size() != 4 || bbox.contains(null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "bbox должен содержать 4 числа: minLon,minLat,maxLon,maxLat");
        }
        double minLon = bbox.get(0);
        double minLat = bbox.get(1);
        double maxLon = bbox.get(2);
        double maxLat = bbox.get(3);

        if (minLon < -180 || maxLon > 180
                || minLat < -90 || maxLat > 90
                || minLon >= maxLon || minLat >= maxLat) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "bbox вне допустимых границ или min не меньше max");
        }
        return service.findPublishedInBbox(minLon, minLat, maxLon, maxLat);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Карточка опубликованного водоёма")
    @ApiResponse(responseCode = "200", description = "Карточка найдена")
    @ApiResponse(responseCode = "404", description = "Водоём не найден или не опубликован")
    public WaterBodyCard card(@PathVariable Long id) {
        return service.findPublishedCard(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Водоём не найден"));
    }
}
