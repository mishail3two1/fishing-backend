package com.fishing.backend.catches;

import com.fishing.backend.catches.dto.CatchResponse;
import com.fishing.backend.catches.dto.CreateCatchRequest;
import com.fishing.backend.catches.dto.UpdateCatchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatchService {
    private final CatchRepository catchRepo;

    @Transactional
    public CatchResponse create(CreateCatchRequest req) {
        if (req.id() != null) {
            Optional<CatchEntity> existing = catchRepo.findById(req.id());
            if (existing.isPresent()) {
                return mapToResponse(existing.get());
            }
        }

        CatchEntity newCatch = new CatchEntity();
        newCatch.setId(req.id());
        newCatch.setUserId(req.userId());
        newCatch.setWaterBodyId(req.waterBodyId());
        newCatch.setSpeciesId(req.speciesId());
        newCatch.setCaughtAt(req.caughtAt());
        newCatch.setWeightG(req.weightG());
        newCatch.setLengthCm(req.lengthCm());
        newCatch.setBait(req.bait());
        newCatch.setRig(req.rig());
        newCatch.setNotes(req.notes());
        newCatch.setVisibility(req.visibility() != null ? req.visibility() : "PRIVATE");
        newCatch.setWeather(req.weather());

        CatchEntity saved = catchRepo.save(newCatch);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CatchResponse getById(UUID id) {
        CatchEntity entity = catchRepo.findById(id).orElseThrow(
                () -> new RuntimeException("Catch not found with id: " + id));

        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CatchResponse> getAll(UUID userId) {
        // Пока просто находим все уловы пользователя (позже добавим фильтры и пагинацию)
        return catchRepo
                .findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CatchResponse update(UUID id, UpdateCatchRequest req) {
        CatchEntity entity = catchRepo.findById(id).orElseThrow(
                () -> new RuntimeException("Catch not found with id: " + id));

        if (req.waterBodyId() != null) entity.setWaterBodyId(req.waterBodyId());
        if (req.speciesId() != null) entity.setSpeciesId(req.speciesId());
        if (req.caughtAt() != null) entity.setCaughtAt(req.caughtAt());
        if (req.weightG() != null) entity.setWeightG(req.weightG());
        if (req.lengthCm() != null) entity.setLengthCm(req.lengthCm());
        if (req.bait() != null) entity.setBait(req.bait());
        if (req.rig() != null) entity.setRig(req.rig());
        if (req.notes() != null) entity.setNotes(req.notes());
        if (req.visibility() != null) entity.setVisibility(req.visibility());
        if (req.weather() != null) entity.setWeather(req.weather());

        CatchEntity saved = catchRepo.save(entity);
        return mapToResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        CatchEntity entity = catchRepo.findById(id).orElseThrow(
                () -> new RuntimeException("Catch not found with id: " + id));

        entity.setDeletedAt(LocalDateTime.now());
        catchRepo.save(entity);
    }

    private CatchResponse mapToResponse(CatchEntity entity) {
        return new CatchResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getWaterBodyId(),
                entity.getSpeciesId(),
                entity.getCaughtAt(),
                entity.getWeightG(),
                entity.getLengthCm(),
                entity.getBait(),
                entity.getRig(),
                entity.getNotes(),
                entity.getVisibility(),
                entity.getWeather(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}