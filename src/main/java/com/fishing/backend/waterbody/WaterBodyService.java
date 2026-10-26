package com.fishing.backend.waterbody;

import com.fishing.backend.waterbody.dto.WaterBodyCard;
import com.fishing.backend.waterbody.dto.WaterBodyMapItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                        waterBody.getVerifiedAt()));
    }
}
