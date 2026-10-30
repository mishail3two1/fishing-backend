package com.fishing.backend.catches;

import com.fishing.backend.catches.dto.CatchResponse;
import com.fishing.backend.catches.dto.CreateCatchRequest;
import com.fishing.backend.catches.dto.UpdateCatchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catches")
@RequiredArgsConstructor
@Tag(name = "Уловы", description = "Создание, просмотр, изменение и удаление уловов")
public class CatchController {

    private final CatchService catchService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать новый улов", description = "Создает улов. Если передан id, проверяет на дубликат (идемпотентность).")
    @ApiResponse(responseCode = "201", description = "Улов успешно создан")
    @ApiResponse(responseCode = "400", description = "Некорректные данные запроса")
    public CatchResponse create(@Valid @RequestBody CreateCatchRequest req) {
        return catchService.create(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить улов по ID")
    @ApiResponse(responseCode = "200", description = "Улов найден")
    @ApiResponse(responseCode = "404", description = "Улов не найден")
    public CatchResponse getById(
            @Parameter(description = "UUID улова", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        return catchService.getById(id);
    }

    @Operation(summary = "Получить историю уловов пользователя", description = "Возвращает список всех неудаленных уловов указанного пользователя.")
    @ApiResponse(responseCode = "200", description = "Список уловов")
    @GetMapping
    public List<CatchResponse> getAll(
            @Parameter(description = "UUID пользователя", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam UUID userId) {
        return catchService.getAll(userId);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить улов", description = "Частичное обновление полей улова. Передаются только те поля, которые нужно изменить.")
    @ApiResponse(responseCode = "200", description = "Улов обновлен")
    @ApiResponse(responseCode = "404", description = "Улов не найден")
    public CatchResponse update(
            @Parameter(description = "UUID улова", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @RequestBody UpdateCatchRequest req) {
        return catchService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить улов", description = "Мягкое удаление: улов помечается как удаленный и больше не возвращается в запросах.")
    @ApiResponse(responseCode = "204", description = "Улов удален")
    @ApiResponse(responseCode = "404", description = "Улов не найден")
    public void delete(
            @Parameter(description = "UUID улова", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        catchService.delete(id);
    }
}