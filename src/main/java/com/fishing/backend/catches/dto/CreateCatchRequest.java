package com.fishing.backend.catches.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO для создания нового улова.
 * Содержит только те поля, которые клиент имеет право передать.
 */
public record CreateCatchRequest(
        // Необязательный ID для идемпотентности (офлайн-режим)
        UUID id,

        // Временно! Пока нет авторизации, клиент передает userId. Позже будем брать из JWT.
        @NotNull(message = "userId is required")
        UUID userId,

        // ID водоема (может быть null, если ловил не на официальном водоеме)
        Long waterBodyId,

        // ID рыбы (может быть null, если не знает)
        Long speciesId,

        // Когда поймал. Обязательное поле.
        @NotNull(message = "caughtAt is required")
        LocalDateTime caughtAt,

        // Вес в граммах
        Integer weightG,

        // Длина в см
        Double lengthCm,

        // Приманка
        String bait,

        // Проводка
        String rig,

        // Заметки
        String notes,

        // Видимость (по умолчанию PRIVATE, если null)
        String visibility,

        // Погода (пока строка, позже JSON)
        String weather
) {}