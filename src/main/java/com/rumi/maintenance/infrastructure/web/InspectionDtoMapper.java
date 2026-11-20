package com.rumi.maintenance.infrastructure.web;

import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionFinding;
import com.rumi.maintenance.infrastructure.web.dto.InspectionFindingResponseDto;
import com.rumi.maintenance.infrastructure.web.dto.InspectionResponseDto;
import org.springframework.stereotype.Component;

@Component
public class InspectionDtoMapper {

    public InspectionResponseDto toResponse(Inspection inspection) {
        return new InspectionResponseDto(
                inspection.getId(),
                inspection.getBuildingId(),
                inspection.getEngineerName(),
                inspection.getScheduledDate(),
                inspection.getStatus(),
                inspection.highestSeverity(),
                inspection.getCreatedAt(),
                inspection.getStartedAt(),
                inspection.getCompletedAt(),
                inspection.getFindings().stream().map(this::toResponse).toList()
        );
    }

    private InspectionFindingResponseDto toResponse(InspectionFinding finding) {
        return new InspectionFindingResponseDto(
                finding.getId(),
                finding.getNotes(),
                finding.getSeverity(),
                finding.getEvidence().url(),
                finding.getEvidence().takenAt(),
                finding.getRecordedAt()
        );
    }
}
