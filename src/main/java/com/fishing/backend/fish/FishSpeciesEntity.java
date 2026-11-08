package com.fishing.backend.fish;

import jakarta.persistence.*;

import java.math.BigDecimal;

// Сущность таблицы fish_species (только чтение). description, slug и даты не отображены: в справочнике они не нужны.
@Entity
@Table(name = "fish_species")
public class FishSpeciesEntity {

    @Id
    private Long id;

    private String nameRu;

    private String nameLat;

    private BigDecimal minCatchSizeCm; // numeric(5,1): точное число, может отсутствовать

    private boolean isProtected;

    private String releaseAdvice;

    public Long getId() { return id; }
    public String getNameRu() { return nameRu; }
    public String getNameLat() { return nameLat; }
    public BigDecimal getMinCatchSizeCm() { return minCatchSizeCm; }
    public boolean isProtected() { return isProtected; }
    public String getReleaseAdvice() { return releaseAdvice; }
}
