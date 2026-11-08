package com.fishing.backend.fish;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FishSpeciesRepository extends JpaRepository<FishSpeciesEntity, Long> {

    List<FishSpeciesEntity> findAllByOrderByNameRuAsc();


}
