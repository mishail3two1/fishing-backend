package com.fishing.backend.waterbody.dto;

import com.fishing.backend.waterbody.AccessType;
import com.fishing.backend.waterbody.WaterType;
import jakarta.validation.constraints.*;

// Данные водоёма от админа: и для создания, и для изменения (PUT — полная замена всех полей).
public record AdminWaterBodyRequest(
        @NotBlank @Size(max = 200) String name,
        // латиница, цифры, дефисы: smolino, uvildy-north; по slug повторный импорт CSV узнаёт водоём
        @NotBlank @Size(max = 200) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*") String slug,
        @NotNull WaterType waterType,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double lon,
        String description,
        @Size(max = 150) String district,
        @NotNull AccessType accessType,
        String accessNotes,
        Boolean hasParking,
        String parkingNotes,
        String rulesNotes,
        @NotBlank String dataSource,
        // не прислали — false: новый водоём по умолчанию черновик
        boolean isPublished) {
}
