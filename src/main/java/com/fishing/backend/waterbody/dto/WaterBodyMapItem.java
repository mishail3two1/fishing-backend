package com.fishing.backend.waterbody.dto;

import com.fishing.backend.waterbody.WaterType;

// Лёгкий водоём для маркера на карте: только то, что нужно для отображения точки.
public record WaterBodyMapItem(Long id, String name, WaterType waterType, double lat, double lon) {
}