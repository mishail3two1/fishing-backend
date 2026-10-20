package com.fishing.backend.waterbody;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaterBodyRepository extends JpaRepository<WaterBodyEntity, Long> {

    List<WaterBodyEntity> findAllByIsPublishedTrue();

}