package com.fishing.backend.waterbody;

import com.fishing.backend.fish.FishSpeciesEntity;
import jakarta.persistence.*;

// Строка таблицы water_body_fish: какой вид водится в водоёме и сколько его.
// Своего id нет: запись существует только внутри водоёма (@ElementCollection в WaterBodyEntity).
@Embeddable
public class WaterBodyFish {

    @ManyToOne
    @JoinColumn(name = "species_id")
    private FishSpeciesEntity species;

    @Enumerated(EnumType.STRING)
    private Abundance abundance;

    public FishSpeciesEntity getSpecies() { return species; }
    public Abundance getAbundance() { return abundance; }
}
