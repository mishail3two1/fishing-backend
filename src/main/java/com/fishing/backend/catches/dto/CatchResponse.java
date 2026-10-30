package com.fishing.backend.catches.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CatchResponse(
        // Идентификаторы
        UUID id,
        UUID userId,
        Long waterBodyId,
        Long speciesId,

        // Данные улова
        LocalDateTime caughtAt,
        Integer weightG,
        Double lengthCm,
        String bait,
        String rig,
        String notes,

        // Служебные данные
        String visibility,
        String weather,

        // Когда создан и обновлен (полезно для клиента)
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}