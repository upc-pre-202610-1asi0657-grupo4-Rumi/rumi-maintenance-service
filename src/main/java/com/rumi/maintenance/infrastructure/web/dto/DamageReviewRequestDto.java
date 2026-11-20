package com.rumi.maintenance.infrastructure.web.dto;

import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.FindingSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "DamageReviewRequest", description = "Decision of the administrator about a damage report")
public record DamageReviewRequestDto(
        @Schema(description = "LOW, MEDIUM or HIGH", example = "HIGH", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        FindingSeverity severity,
        @Schema(description = "UNDER_REVIEW to keep working on it, RESOLVED to close it", example = "UNDER_REVIEW",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        DamageReportStatus status
) {
}
