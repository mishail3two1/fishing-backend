package com.fishing.backend.fish;

import com.fishing.backend.fish.dto.FishSpeciesItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FishSpeciesService {

    private final FishSpeciesRepository repository;

    public FishSpeciesService(FishSpeciesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FishSpeciesItem> findAll() {
        return repository.findAllByOrderByNameRuAsc().stream()
                .map(fish -> new FishSpeciesItem(
                        fish.getId(),
                        fish.getNameRu(),
                        fish.getNameLat(),
                        fish.getMinCatchSizeCm(),
                        fish.isProtected(),
                        fish.getReleaseAdvice()))
                .toList();
    }
}
