package com.fishing.backend.waterbody;

import com.fishing.backend.waterbody.dto.WaterBodyCard;
import com.fishing.backend.waterbody.dto.WaterBodyMapItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fishing.backend.waterbody.dto.WaterBodyFishItem;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WaterBodyService {

    private final WaterBodyRepository repository;

    public WaterBodyService(WaterBodyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<WaterBodyMapItem> findPublishedInBbox(double minLon, double minLat, double maxLon, double maxLat) {
        List<WaterBodyMapItem> result = new ArrayList<>();
        for (WaterBodyEntity waterBody : repository.findPublishedInBbox(minLon, minLat, maxLon, maxLat)) {
            // в PostGIS у точки x = долгота, y = широта
            double lat = waterBody.getLocation().getY();
            double lon = waterBody.getLocation().getX();
            result.add(new WaterBodyMapItem(waterBody.getId(), waterBody.getName(),
                    waterBody.getWaterType(), lat, lon));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<WaterBodyCard> findPublishedCard(Long id) {
        return repository.findByIdAndIsPublishedTrue(id)
                .map(waterBody -> new WaterBodyCard(
                        waterBody.getId(),
                        waterBody.getName(),
                        waterBody.getWaterType(),
                        waterBody.getLocation().getY(),
                        waterBody.getLocation().getX(),
                        waterBody.getDescription(),
                        waterBody.getDistrict(),
                        waterBody.getAccessType(),
                        waterBody.getAccessNotes(),
                        waterBody.getHasParking(),
                        waterBody.getParkingNotes(),
                        waterBody.getRulesNotes(),
                        waterBody.getVerifiedAt(),
                        toFishItems(waterBody.getFish())));
    }

    // видов в одном водоёме единицы, и они уже загружены, поэтому сортируем в Java;
    // Collator — по правилам русского языка (по кодам Unicode заглавная «Ё» стоит раньше «А»)
    private List<WaterBodyFishItem> toFishItems(List<WaterBodyFish> fish) {
        Collator russian = Collator.getInstance(Locale.forLanguageTag("ru"));
        return fish.stream()
                .map(item -> new WaterBodyFishItem(
                        item.getSpecies().getId(),
                        item.getSpecies().getNameRu(),
                        item.getSpecies().isProtected(),
                        item.getAbundance()))
                .sorted(Comparator.comparing(WaterBodyFishItem::nameRu, russian))
                .toList();
    }
}
