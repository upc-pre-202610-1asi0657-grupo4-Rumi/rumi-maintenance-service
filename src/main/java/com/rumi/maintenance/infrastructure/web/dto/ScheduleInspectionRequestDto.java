package com.rumi.maintenance.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "ScheduleInspectionRequest", description = "Inspection to add to the calendar of a building")
public record ScheduleInspectionRequestDto(
        @Schema(description = "Building to inspect", example = "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UUID buildingId,
        @Schema(description = "Engineer responsible for the visit", example = "Ing. Carla Mendoza",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 120)
        String engineerName,
        @Schema(description = "Date of the visit; today or later", example = "2026-11-27",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        LocalDate scheduledDate,
        @Schema(description = "Damage report that this inspection evaluates, if any. It must belong to the same "
                + "building and moves to UNDER_REVIEW", example = "8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13",
                nullable = true)
        UUID damageReportId
) {
}
