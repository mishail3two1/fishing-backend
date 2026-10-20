package com.fishing.backend.waterbody;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

// Сущность таблицы water_bodies. Отображены только поля, нужные для карты:
// остальные колонки Hibernate просто не читает.
@Entity
@Table(name = "water_bodies")
public class WaterBodyEntity {

    @Id
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private WaterType waterType;

    private Point location;

    private boolean isPublished;

    public Long getId() { return id; }
    public String getName() { return name; }
    public WaterType getWaterType() { return waterType; }
    public Point getLocation() { return location; }
}