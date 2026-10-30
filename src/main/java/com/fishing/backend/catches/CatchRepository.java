package com.fishing.backend.catches;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CatchRepository extends JpaRepository<CatchEntity, UUID> {
    List<CatchEntity> findByUserIdAndDeletedAtIsNull(UUID userId);
}