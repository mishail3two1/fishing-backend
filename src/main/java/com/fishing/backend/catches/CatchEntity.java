package com.fishing.backend.catches;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "catches")
@Getter
@Setter
public class CatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "water_body_id")
    private Long waterBodyId;

    @Column(name = "species_id")
    private Long speciesId;

    @Column(name = "species_source", nullable = false)
    private String speciesSource = "USER";

    @Column(name = "ai_confidence", columnDefinition = "numeric(4,3)")
    private Double aiConfidence;

    @Column(name = "caught_at", nullable = false)
    private LocalDateTime caughtAt;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "weight_g")
    private Integer weightG;

    @Column(name = "length_cm", columnDefinition = "numeric(5,1)")
    private Double lengthCm;

    private String bait;

    private String rig;

    private String notes;

    @Column(nullable = false)
    private String visibility = "PRIVATE";

    @Column(columnDefinition = "jsonb")
    private String weather;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}