package com.fishing.backend.waterbody;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WaterBodyRepository extends JpaRepository<WaterBodyEntity, Long> {

    // нативный SQL: оператора && нет в JPQL, а именно он использует GiST-индекс по location;
    // SELECT *, чтобы новые поля сущности не ломали маппинг (лишние колонки Hibernate игнорирует)
    @Query(value = """
            SELECT *
            FROM water_bodies
            WHERE is_published
              AND location && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
            """, nativeQuery = true)
    List<WaterBodyEntity> findPublishedInBbox(@Param("minLon") double minLon,
                                              @Param("minLat") double minLat,
                                              @Param("maxLon") double maxLon,
                                              @Param("maxLat") double maxLat);

    Optional<WaterBodyEntity> findByIdAndIsPublishedTrue(Long id);
}
