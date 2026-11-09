package com.fishing.backend.waterbody;

import com.fishing.backend.waterbody.dto.AdminWaterBodyRequest;
import com.fishing.backend.waterbody.dto.AdminWaterBodyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Админка водоёмов. Доступ только ADMIN — правило /api/v1/admin/** в SecurityConfig.
@RestController
@RequestMapping("/api/v1/admin/water-bodies")
@SecurityRequirement(name = "bearerAuth")
public class AdminWaterBodyController {

    private final AdminWaterBodyService service;

    public AdminWaterBodyController(AdminWaterBodyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать водоём (по умолчанию черновик)")
    @ApiResponse(responseCode = "201", description = "Водоём создан")
    @ApiResponse(responseCode = "400", description = "Некорректные данные")
    @ApiResponse(responseCode = "401", description = "Требуется вход")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @ApiResponse(responseCode = "409", description = "slug уже занят")
    public AdminWaterBodyResponse create(@Valid @RequestBody AdminWaterBodyRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить водоём целиком: в том числе опубликовать или скрыть (isPublished)")
    @ApiResponse(responseCode = "200", description = "Водоём изменён")
    @ApiResponse(responseCode = "400", description = "Некорректные данные")
    @ApiResponse(responseCode = "404", description = "Водоём не найден")
    @ApiResponse(responseCode = "409", description = "slug уже занят другим водоёмом")
    public AdminWaterBodyResponse update(@PathVariable Long id, @Valid @RequestBody AdminWaterBodyRequest request) {
        return service.update(id, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Водоём не найден"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Водоём для админа, в том числе черновик")
    @ApiResponse(responseCode = "200", description = "Водоём")
    @ApiResponse(responseCode = "404", description = "Водоём не найден")
    public AdminWaterBodyResponse get(@PathVariable Long id) {
        return service.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Водоём не найден"));
    }
}
