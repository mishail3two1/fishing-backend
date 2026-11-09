package com.fishing.backend.waterbody.dto;

import com.fishing.backend.waterbody.Abundance;

// Вид рыбы на карточке водоёма. Подробности о виде — в справочнике GET /fish-species по speciesId.
public record WaterBodyFishItem(Long speciesId, String nameRu, boolean isProtected, Abundance abundance) {

}
