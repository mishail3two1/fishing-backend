package com.fishing.backend.waterbody.dto;

import com.fishing.backend.waterbody.AccessType;
import com.fishing.backend.waterbody.WaterType;

import java.time.Instant;

// Водоём глазами админа: в отличие от публичной карточки здесь есть slug, dataSource и флаг публикации.
public record AdminWaterBodyResponse(
        Long id,
        String name,
        String slug,
        WaterType waterType,
        double lat,
        double lon,
        String description,
        String district,
        AccessType accessType,
        String accessNotes,
        Boolean hasParking,
        String parkingNotes,
        String rulesNotes,
        String dataSource,
        boolean isPublished,
        Instant verifiedAt) {
}
