
package com.fishing.backend.waterbody.dto;

import com.fishing.backend.waterbody.AccessType;
import com.fishing.backend.waterbody.WaterType;

import java.time.Instant;

// Карточка водоёма. Рыба, последние уловы и погода добавятся отдельными задачами.
// data_source не отдаём: источники данных видят только админы.
public record WaterBodyCard(
        Long id,
        String name,
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
        Instant verifiedAt) {
}
