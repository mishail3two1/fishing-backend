package com.fishing.backend.waterbody;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Сущность таблицы water_bodies: поля для карты, карточки и админки, список рыб (water_body_fish).
// Даты создания и обновления не отображены: их ставит база (DEFAULT now() и триггер).
@Entity
@Table(name = "water_bodies")
public class WaterBodyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String slug; // для импорта CSV: по нему повторный импорт узнаёт водоём

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

    private String dataSource; // откуда взяты данные; видят только админы

    // строки water_body_fish этого водоёма; загружаются при первом обращении, поэтому только внутри транзакции сервиса
    @ElementCollection
    @CollectionTable(name = "water_body_fish", joinColumns = @JoinColumn(name = "water_body_id"))
    private List<WaterBodyFish> fish = new ArrayList<>();

    private boolean isPublished;

    // для Hibernate и для создания нового водоёма; значения полей задаёт update(...)
    public WaterBodyEntity() {
    }

    // все редактируемые поля в одном месте: и создание, и изменение водоёма идут через этот метод
    public void update(String name, String slug, WaterType waterType, Point location,
                       String description, String district, AccessType accessType, String accessNotes,
                       Boolean hasParking, String parkingNotes, String rulesNotes,
                       String dataSource, boolean isPublished, Instant verifiedAt) {
        this.name = name;
        this.slug = slug;
        this.waterType = waterType;
        this.location = location;
        this.description = description;
        this.district = district;
        this.accessType = accessType;
        this.accessNotes = accessNotes;
        this.hasParking = hasParking;
        this.parkingNotes = parkingNotes;
        this.rulesNotes = rulesNotes;
        this.dataSource = dataSource;
        this.isPublished = isPublished;
        this.verifiedAt = verifiedAt;
    }

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
    public List<WaterBodyFish> getFish() { return fish; }
    public String getSlug() { return slug; }
    public String getDataSource() { return dataSource; }
    public boolean isPublished() { return isPublished; }
}
