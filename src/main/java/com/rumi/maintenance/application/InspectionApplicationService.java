package com.rumi.maintenance.application;

import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionStatus;
import com.rumi.maintenance.domain.model.PhotoEvidence;
import com.rumi.maintenance.domain.repository.DamageReportRepository;
import com.rumi.maintenance.domain.repository.InspectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InspectionApplicationService {

    private final InspectionRepository inspectionRepository;
    private final DamageReportRepository damageReportRepository;
    private final Clock clock;

    public InspectionApplicationService(
            InspectionRepository inspectionRepository,
            DamageReportRepository damageReportRepository,
            Clock clock
    ) {
        this.inspectionRepository = inspectionRepository;
        this.damageReportRepository = damageReportRepository;
        this.clock = clock;
    }

    /**
     * Schedules an inspection with its responsible engineer (US17). It is registered in the calendar of
     * the building as PENDING. When the command names a damage report of the same building, the report is
     * linked to the inspection and moves to UNDER_REVIEW.
     */
    @Transactional
    public Inspection scheduleInspection(ScheduleInspectionCommand command) {
        if (command.buildingId() == null) {
            throw new IllegalArgumentException("Building id is required");
        }
        DamageReport report = null;
        if (command.damageReportId() != null) {
            report = damageReportRepository.findById(command.damageReportId())
                    .orElseThrow(() -> new DamageReportNotFoundException(command.damageReportId()));
            if (!report.belongsTo(command.buildingId())) {
                throw new IllegalArgumentException("Damage report " + report.getId()
                        + " belongs to building " + report.getBuildingId());
            }
        }
        Inspection inspection = inspectionRepository.save(Inspection.schedule(
                UUID.randomUUID(),
                command.buildingId(),
                command.engineerName(),
                command.scheduledDate(),
                LocalDate.now(clock),
                clock.instant()
        ));
        if (report != null) {
            report.linkInspection(inspection.getId(), clock.instant());
            damageReportRepository.save(report);
        }
        return inspection;
    }

    /**
     * Calendar of inspections of a building, ordered by scheduled date, optionally only those with one status.
     */
    public List<Inspection> listInspections(UUID buildingId, InspectionStatus status) {
        if (buildingId == null) {
            throw new IllegalArgumentException("Building id is required");
        }
        return inspectionRepository.findByBuildingId(buildingId, status);
    }

    public Inspection getInspection(UUID inspectionId) {
        return inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
    }

    /**
     * The engineer starts the visit, so the inspection is in progress and findings can be recorded.
     */
    @Transactional
    public Inspection startInspection(UUID inspectionId) {
        Inspection inspection = getInspection(inspectionId);
        inspection.start(clock.instant());
        return inspectionRepository.save(inspection);
    }

    /**
     * Records a finding with its notes, severity and photo (US18). The finding is associated with the
     * building of the inspection and receives the current date.
     */
    @Transactional
    public Inspection recordFinding(RecordFindingCommand command) {
        Inspection inspection = getInspection(command.inspectionId());
        inspection.recordFinding(
                UUID.randomUUID(),
                command.notes(),
                command.severity(),
                new PhotoEvidence(command.photoUrl(), command.photoTakenAt()),
                clock.instant()
        );
        return inspectionRepository.save(inspection);
    }

    /**
     * Ends the visit once its findings are recorded.
     */
    @Transactional
    public Inspection completeInspection(UUID inspectionId) {
        Inspection inspection = getInspection(inspectionId);
        inspection.complete(clock.instant());
        return inspectionRepository.save(inspection);
    }
}
