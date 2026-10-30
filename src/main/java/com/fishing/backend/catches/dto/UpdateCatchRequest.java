package com.fishing.backend.catches.dto;

import java.time.LocalDateTime;

// DTO для обновления улова.

public record UpdateCatchRequest(
        Long waterBodyId,
        Long speciesId,
        LocalDateTime caughtAt,
        Integer weightG,
        Double lengthCm,
        String bait,
        String rig,
        String notes,
        String visibility,
        String weather
) {}