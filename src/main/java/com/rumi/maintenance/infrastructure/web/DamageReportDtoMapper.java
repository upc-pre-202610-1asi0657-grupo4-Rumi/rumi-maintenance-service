package com.rumi.maintenance.infrastructure.web;

import com.rumi.maintenance.application.DamageBoard;
import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.FindingSeverity;
import com.rumi.maintenance.infrastructure.web.dto.DamageBoardResponseDto;
import com.rumi.maintenance.infrastructure.web.dto.DamageReportResponseDto;
import org.springframework.stereotype.Component;

@Component
public class DamageReportDtoMapper {

    static final String UNASSESSED = "UNASSESSED";

    public DamageReportResponseDto toResponse(DamageReport report) {
        return new DamageReportResponseDto(
                report.getId(),
                report.getBuildingId(),
                report.getResidentUserId(),
                report.getDescription(),
                report.getZone(),
                report.getEvidence().url(),
                report.getEvidence().takenAt(),
                report.getSeverity(),
                report.getStatus(),
                report.getGeneratedInspectionId(),
                report.getReportedAt(),
                report.getReviewedAt(),
                report.getResolvedAt()
        );
    }

    public DamageBoardResponseDto toResponse(DamageBoard board) {
        return new DamageBoardResponseDto(
                board.buildingId(),
                board.totalReports(),
                board.zones().stream()
                        .map(zone -> new DamageBoardResponseDto.Zone(
                                zone.zone(),
                                zone.totalReports(),
                                zone.highestSeverity() == null ? null : zone.highestSeverity().name(),
                                zone.groups().stream()
                                        .map(group -> new DamageBoardResponseDto.SeverityGroup(
                                                label(group.severity()),
                                                group.reports().stream().map(this::toResponse).toList()))
                                        .toList()))
                        .toList()
        );
    }

    private static String label(FindingSeverity severity) {
        return severity == null ? UNASSESSED : severity.name();
    }
}
