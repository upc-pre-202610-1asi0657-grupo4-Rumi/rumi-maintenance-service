package com.rumi.maintenance.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "DamageBoardResponse",
        description = "Damage reports of a building grouped by zone and severity, most severe zones first")
public record DamageBoardResponseDto(
        @Schema(description = "Building of the board", example = "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d")
        UUID buildingId,
        @Schema(description = "Number of reports on the board", example = "3")
        int totalReports,
        @Schema(description = "Zones with damage, most severe first")
        List<Zone> zones
) {

    @Schema(name = "DamageBoardZone", description = "Damage reports of one zone")
    public record Zone(
            @Schema(description = "Zone of the building", example = "FLOOR-2-EAST")
            String zone,
            @Schema(description = "Number of reports in the zone", example = "2")
            int totalReports,
            @Schema(description = "Most severe assessed damage of the zone; null when none is assessed",
                    example = "HIGH", nullable = true)
            String highestSeverity,
            @Schema(description = "Reports grouped by severity: HIGH, MEDIUM, LOW and finally UNASSESSED")
            List<SeverityGroup> groups
    ) {
    }

    @Schema(name = "DamageBoardSeverityGroup", description = "Reports of one zone with the same severity")
    public record SeverityGroup(
            @Schema(description = "HIGH, MEDIUM, LOW, or UNASSESSED for reports not reviewed yet", example = "HIGH")
            String severity,
            @Schema(description = "Reports of the group, newest first")
            List<DamageReportResponseDto> reports
    ) {
    }
}
