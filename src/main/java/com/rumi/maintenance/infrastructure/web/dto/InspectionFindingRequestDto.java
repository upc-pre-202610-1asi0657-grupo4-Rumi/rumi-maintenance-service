package com.rumi.maintenance.infrastructure.web.dto;

import com.rumi.maintenance.domain.model.FindingSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(name = "InspectionFindingRequest", description = "Finding recorded during an inspection in progress")
public record InspectionFindingRequestDto(
        @Schema(description = "What the engineer found", example = "Diagonal crack of 3 mm in the beam of axis B-2",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 500)
        String notes,
        @Schema(description = "LOW, MEDIUM or HIGH", example = "HIGH", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        FindingSeverity severity,
        @Schema(description = "http or https address of the photo of the finding",
                example = "https://storage.example.com/rumi/inspections/beam-b2.jpg",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 500)
        String photoUrl,
        @Schema(description = "When the photo was taken (UTC)", example = "2026-11-27T15:10:00Z", nullable = true)
        Instant photoTakenAt
) {
}
