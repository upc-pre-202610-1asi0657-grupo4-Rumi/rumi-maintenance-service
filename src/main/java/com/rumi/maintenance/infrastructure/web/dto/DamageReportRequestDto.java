package com.rumi.maintenance.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "DamageReportRequest", description = "Visible damage reported by a resident")
public record DamageReportRequestDto(
        @Schema(description = "Building where the damage is", example = "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UUID buildingId,
        @Schema(description = "What the resident sees", example = "Crack in the wall next to the elevator",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 500)
        String description,
        @Schema(description = "Where the damage is (floor, area or zone of the building)", example = "FLOOR-2-EAST",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 50)
        String zone,
        @Schema(description = "http or https address of the photo of the damage",
                example = "https://storage.example.com/rumi/damage/floor2-wall.jpg",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 500)
        String photoUrl,
        @Schema(description = "When the photo was taken (UTC)", example = "2026-11-20T13:45:00Z", nullable = true)
        Instant photoTakenAt
) {
}
