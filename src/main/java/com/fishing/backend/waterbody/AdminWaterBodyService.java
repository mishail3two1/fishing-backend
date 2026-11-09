package com.fishing.backend.waterbody;

import com.fishing.backend.waterbody.dto.AdminWaterBodyRequest;
import com.fishing.backend.waterbody.dto.AdminWaterBodyResponse;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AdminWaterBodyService {

    // SRID 4326, как у колонки location: иначе PostGIS не примет точку
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);

    private final WaterBodyRepository repository;

    public AdminWaterBodyService(WaterBodyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AdminWaterBodyResponse create(AdminWaterBodyRequest request) {
        if (repository.existsBySlug(request.slug())) {
            throw new SlugAlreadyUsedException();
        }
        WaterBodyEntity waterBody = new WaterBodyEntity();
        apply(waterBody, request);
        repository.save(waterBody);
        return toResponse(waterBody);
    }

    @Transactional
    public Optional<AdminWaterBodyResponse> update(Long id, AdminWaterBodyRequest request) {
        return repository.findById(id).map(waterBody -> {
            if (repository.existsBySlugAndIdNot(request.slug(), id)) {
                throw new SlugAlreadyUsedException();
            }
            apply(waterBody, request);
            // save не нужен: сущность загружена в этой транзакции, изменения Hibernate запишет сам при её завершении
            return toResponse(waterBody);
        });
    }

    @Transactional(readOnly = true)
    public Optional<AdminWaterBodyResponse> find(Long id) {
        // в отличие от публичной карточки ищем любой водоём, в том числе черновик
        return repository.findById(id).map(this::toResponse);
    }

    // общее для создания и изменения: все поля из запроса, точка из lat/lon, verifiedAt — момент правки
    private void apply(WaterBodyEntity waterBody, AdminWaterBodyRequest request) {
        // в JTS x = долгота, y = широта
        Point location = GEOMETRY.createPoint(new Coordinate(request.lon(), request.lat()));
        waterBody.update(request.name().trim(), request.slug(), request.waterType(), location,
                request.description(), request.district(), request.accessType(), request.accessNotes(),
                request.hasParking(), request.parkingNotes(), request.rulesNotes(),
                request.dataSource().trim(), request.isPublished(), Instant.now());
    }

    private AdminWaterBodyResponse toResponse(WaterBodyEntity waterBody) {
        return new AdminWaterBodyResponse(
                waterBody.getId(),
                waterBody.getName(),
                waterBody.getSlug(),
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
                waterBody.getDataSource(),
                waterBody.isPublished(),
                waterBody.getVerifiedAt());
    }
}
