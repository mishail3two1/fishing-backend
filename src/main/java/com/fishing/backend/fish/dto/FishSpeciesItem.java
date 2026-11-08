package com.fishing.backend.fish.dto;

import java.math.BigDecimal;

// Вид рыбы для справочника и выбора рыбы в улове.
public record FishSpeciesItem(
        Long id,
        String nameRu,
        String nameLat,
        BigDecimal minCatchSizeCm,
        boolean isProtected,
        String releaseAdvice) {
}
