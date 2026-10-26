package com.fishing.backend.waterbody;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

// Сущность таблицы water_bodies. Отображены поля для карты и карточки водоёма:
// служебные колонки (slug, data_source, даты создания) Hibernate не читает.
@Entity
@Table(name = "water_bodies")
public class WaterBodyEntity {

    @Id
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private WaterType waterType;

    private Point location;

    private String description;

    private String district;

    @Enumerated(EnumType.STRING)
    private AccessType accessType;

    private String accessNotes;

    private Boolean hasParking; // NULL = неизвестно

    private String parkingNotes;

    private String rulesNotes;

    private Instant verifiedAt;

    private boolean isPublished;

    public Long getId() { return id; }
    public String getName() { return name; }
    public WaterType getWaterType() { return waterType; }
    public Point getLocation() { return location; }
    public String getDescription() { return description; }
    public String getDistrict() { return district; }
    public AccessType getAccessType() { return accessType; }
    public String getAccessNotes() { return accessNotes; }
    public Boolean getHasParking() { return hasParking; }
    public String getParkingNotes() { return parkingNotes; }
    public String getRulesNotes() { return rulesNotes; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
