package com.rumi.maintenance.application;

import com.rumi.maintenance.MaintenanceFixtures;
import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.FindingSeverity;
import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionStatus;
import com.rumi.maintenance.domain.model.MaintenanceStateException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InspectionApplicationServiceTest {

    private static final UUID BUILDING_ID = MaintenanceFixtures.BUILDING_ID;

    private final InMemoryInspectionRepository inspectionRepository = new InMemoryInspectionRepository();
    private final InMemoryDamageReportRepository damageReportRepository = new InMemoryDamageReportRepository();
    private final InspectionApplicationService service = new InspectionApplicationService(
            inspectionRepository, damageReportRepository,
            Clock.fixed(MaintenanceFixtures.NOW, ZoneId.of("America/Lima")));

    private ScheduleInspectionCommand schedule(UUID damageReportId) {
        return new ScheduleInspectionCommand(BUILDING_ID, "Ing. Carla Mendoza", MaintenanceFixtures.NEXT_WEEK,
                damageReportId);
    }

    private RecordFindingCommand finding(UUID inspectionId, FindingSeverity severity) {
        return new RecordFindingCommand(inspectionId, "Diagonal crack of 3 mm in the beam of axis B-2", severity,
                MaintenanceFixtures.PHOTO_URL, null);
    }

    @Test
    void schedulesAPendingInspectionInTheCalendarOfTheBuilding() {
        Inspection inspection = service.scheduleInspection(schedule(null));

        assertThat(inspection.getStatus()).isEqualTo(InspectionStatus.PENDING);
        assertThat(inspection.getBuildingId()).isEqualTo(BUILDING_ID);
        assertThat(inspection.getCreatedAt()).isEqualTo(MaintenanceFixtures.NOW);
        assertThat(inspectionRepository.saved()).containsExactly(inspection);
    }

    @Test
    void usesTheDateOfTheBuildingsToRejectAPastDate() {
        assertThatThrownBy(() -> service.scheduleInspection(new ScheduleInspectionCommand(BUILDING_ID,
                "Ing. Carla Mendoza", MaintenanceFixtures.TODAY.minusDays(1), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled date cannot be in the past");
        assertThat(inspectionRepository.saved()).isEmpty();
    }

    @Test
    void anInspectionScheduledForADamageReportPutsTheReportUnderReview() {
        DamageReport report = damageReportRepository.save(MaintenanceFixtures.reportedDamage("FLOOR-2-EAST"));

        Inspection inspection = service.scheduleInspection(schedule(report.getId()));

        DamageReport stored = damageReportRepository.findById(report.getId()).orElseThrow();
        assertThat(stored.getGeneratedInspectionId()).isEqualTo(inspection.getId());
        assertThat(stored.getStatus()).isEqualTo(DamageReportStatus.UNDER_REVIEW);
    }

    @Test
    void rejectsADamageReportOfAnotherBuildingOrAnUnknownOne() {
        DamageReport other = damageReportRepository.save(MaintenanceFixtures.reportedDamage(
                MaintenanceFixtures.OTHER_BUILDING_ID, "LOBBY", MaintenanceFixtures.NOW));
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.scheduleInspection(schedule(other.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Damage report " + other.getId() + " belongs to building "
                        + MaintenanceFixtures.OTHER_BUILDING_ID);
        assertThatThrownBy(() -> service.scheduleInspection(schedule(unknown)))
                .isInstanceOf(DamageReportNotFoundException.class)
                .hasMessage("Damage report " + unknown + " was not found");
        assertThat(inspectionRepository.saved()).isEmpty();
    }

    @Test
    void listsTheInspectionsOfABuildingByStatus() {
        Inspection pending = service.scheduleInspection(schedule(null));
        Inspection started = service.startInspection(service.scheduleInspection(schedule(null)).getId());
        inspectionRepository.save(Inspection.schedule(UUID.randomUUID(), MaintenanceFixtures.OTHER_BUILDING_ID,
                "Ing. Ana Ruiz", MaintenanceFixtures.NEXT_WEEK, MaintenanceFixtures.TODAY, MaintenanceFixtures.NOW));

        assertThat(service.listInspections(BUILDING_ID, null)).hasSize(2);
        assertThat(service.listInspections(BUILDING_ID, InspectionStatus.PENDING)).containsExactly(pending);
        assertThat(service.listInspections(BUILDING_ID, InspectionStatus.IN_PROGRESS)).containsExactly(started);
        assertThatThrownBy(() -> service.listInspections(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Building id is required");
    }

    @Test
    void runsTheVisitFromStartToCompletionWithItsFindings() {
        UUID inspectionId = service.scheduleInspection(schedule(null)).getId();

        service.startInspection(inspectionId);
        service.recordFinding(finding(inspectionId, FindingSeverity.MEDIUM));
        Inspection withFindings = service.recordFinding(finding(inspectionId, FindingSeverity.HIGH));
        Inspection completed = service.completeInspection(inspectionId);

        assertThat(withFindings.getFindings()).hasSize(2);
        assertThat(withFindings.getFindings().get(0).getRecordedAt()).isEqualTo(MaintenanceFixtures.NOW);
        assertThat(completed.getStatus()).isEqualTo(InspectionStatus.COMPLETED);
        assertThat(completed.highestSeverity()).isEqualTo(FindingSeverity.HIGH);
    }

    @Test
    void aFindingNeedsAnInspectionInProgressAndAWebPhoto() {
        UUID inspectionId = service.scheduleInspection(schedule(null)).getId();

        assertThatThrownBy(() -> service.recordFinding(finding(inspectionId, FindingSeverity.LOW)))
                .isInstanceOf(MaintenanceStateException.class);
        service.startInspection(inspectionId);
        assertThatThrownBy(() -> service.recordFinding(new RecordFindingCommand(inspectionId, "Crack",
                FindingSeverity.LOW, "C:\\photos\\crack.jpg", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Photo URL must be an http or https address");
    }

    @Test
    void anUnknownInspectionIsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.startInspection(unknown))
                .isInstanceOf(InspectionNotFoundException.class)
                .hasMessage("Inspection " + unknown + " was not found");
        assertThatThrownBy(() -> service.completeInspection(unknown))
                .isInstanceOf(InspectionNotFoundException.class);
    }
}
