package com.rumi.maintenance.domain.model;

import com.rumi.maintenance.MaintenanceFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InspectionTest {

    @Test
    void aScheduledInspectionIsPendingInTheCalendarOfTheBuilding() {
        Inspection inspection = MaintenanceFixtures.pendingInspection();

        assertThat(inspection.getStatus()).isEqualTo(InspectionStatus.PENDING);
        assertThat(inspection.getBuildingId()).isEqualTo(MaintenanceFixtures.BUILDING_ID);
        assertThat(inspection.getEngineerName()).isEqualTo("Ing. Carla Mendoza");
        assertThat(inspection.getScheduledDate()).isEqualTo(MaintenanceFixtures.NEXT_WEEK);
        assertThat(inspection.getCreatedAt()).isEqualTo(MaintenanceFixtures.NOW);
        assertThat(inspection.getFindings()).isEmpty();
        assertThat(inspection.highestSeverity()).isNull();
    }

    @Test
    void anInspectionCanBeScheduledForTodayButNotForThePast() {
        Inspection today = Inspection.schedule(UUID.randomUUID(), MaintenanceFixtures.BUILDING_ID, "Ing. Ana Ruiz",
                MaintenanceFixtures.TODAY, MaintenanceFixtures.TODAY, MaintenanceFixtures.NOW);

        assertThat(today.getStatus()).isEqualTo(InspectionStatus.PENDING);
        assertThatThrownBy(() -> Inspection.schedule(UUID.randomUUID(), MaintenanceFixtures.BUILDING_ID,
                "Ing. Ana Ruiz", MaintenanceFixtures.TODAY.minusDays(1), MaintenanceFixtures.TODAY,
                MaintenanceFixtures.NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled date cannot be in the past");
    }

    @Test
    void anInspectionNeedsAnEngineer() {
        assertThatThrownBy(() -> Inspection.schedule(UUID.randomUUID(), MaintenanceFixtures.BUILDING_ID, "  ",
                MaintenanceFixtures.NEXT_WEEK, MaintenanceFixtures.TODAY, MaintenanceFixtures.NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Engineer name is required");
        assertThatThrownBy(() -> Inspection.schedule(UUID.randomUUID(), MaintenanceFixtures.BUILDING_ID,
                "x".repeat(121), MaintenanceFixtures.NEXT_WEEK, MaintenanceFixtures.TODAY, MaintenanceFixtures.NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Engineer name must have at most 120 characters");
    }

    @Test
    void aPendingInspectionStartsOnlyOnce() {
        Inspection inspection = MaintenanceFixtures.pendingInspection();
        Instant at = MaintenanceFixtures.NOW.plusSeconds(60);

        inspection.start(at);

        assertThat(inspection.getStatus()).isEqualTo(InspectionStatus.IN_PROGRESS);
        assertThat(inspection.getStartedAt()).isEqualTo(at);
        assertThatThrownBy(() -> inspection.start(at))
                .isInstanceOf(MaintenanceStateException.class)
                .hasMessage("Inspection " + inspection.getId() + " is IN_PROGRESS and cannot be started");
    }

    @Test
    void findingsAreRecordedWithDateSeverityAndEvidenceWhileTheInspectionIsInProgress() {
        Inspection inspection = MaintenanceFixtures.inProgressInspection();
        Instant at = MaintenanceFixtures.NOW.plusSeconds(120);

        InspectionFinding finding = inspection.recordFinding(UUID.randomUUID(), "Crack in the beam of axis B-2",
                FindingSeverity.MEDIUM, MaintenanceFixtures.photo(), at);
        inspection.recordFinding(UUID.randomUUID(), "Spalled concrete in column C-4", FindingSeverity.HIGH,
                MaintenanceFixtures.photo(), at.plusSeconds(60));

        assertThat(finding.getRecordedAt()).isEqualTo(at);
        assertThat(finding.getSeverity()).isEqualTo(FindingSeverity.MEDIUM);
        assertThat(finding.getEvidence().url()).isEqualTo(MaintenanceFixtures.PHOTO_URL);
        assertThat(inspection.getFindings()).hasSize(2);
        assertThat(inspection.getFindings().get(0)).isEqualTo(finding);
        assertThat(inspection.highestSeverity()).isEqualTo(FindingSeverity.HIGH);
    }

    @Test
    void findingsCannotBeRecordedBeforeTheVisitStarts() {
        Inspection inspection = MaintenanceFixtures.pendingInspection();

        assertThatThrownBy(() -> inspection.recordFinding(UUID.randomUUID(), "Crack", FindingSeverity.LOW,
                MaintenanceFixtures.photo(), MaintenanceFixtures.NOW))
                .isInstanceOf(MaintenanceStateException.class)
                .hasMessage("Inspection " + inspection.getId() + " is PENDING and cannot receive findings");
        assertThat(inspection.getFindings()).isEmpty();
    }

    @Test
    void aFindingNeedsNotesASeverityAndAPhoto() {
        Inspection inspection = MaintenanceFixtures.inProgressInspection();

        assertThatThrownBy(() -> inspection.recordFinding(UUID.randomUUID(), " ", FindingSeverity.LOW,
                MaintenanceFixtures.photo(), MaintenanceFixtures.NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Finding notes are required");
        assertThatThrownBy(() -> inspection.recordFinding(UUID.randomUUID(), "Crack", null,
                MaintenanceFixtures.photo(), MaintenanceFixtures.NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Finding severity is required");
        assertThatThrownBy(() -> inspection.recordFinding(UUID.randomUUID(), "Crack", FindingSeverity.LOW,
                null, MaintenanceFixtures.NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Photo evidence of the finding is required");
        assertThat(inspection.getFindings()).isEmpty();
    }

    @Test
    void anInspectionInProgressIsCompletedOnceItHasFindings() {
        Inspection inspection = MaintenanceFixtures.inspectionWithFinding(FindingSeverity.HIGH);
        Instant at = MaintenanceFixtures.NOW.plusSeconds(3600);

        inspection.complete(at);

        assertThat(inspection.getStatus()).isEqualTo(InspectionStatus.COMPLETED);
        assertThat(inspection.getCompletedAt()).isEqualTo(at);
        assertThatThrownBy(() -> inspection.recordFinding(UUID.randomUUID(), "Late", FindingSeverity.LOW,
                MaintenanceFixtures.photo(), at))
                .isInstanceOf(MaintenanceStateException.class);
    }

    @Test
    void anInspectionWithoutFindingsCannotBeCompleted() {
        Inspection inspection = MaintenanceFixtures.inProgressInspection();

        assertThatThrownBy(() -> inspection.complete(MaintenanceFixtures.NOW))
                .isInstanceOf(MaintenanceStateException.class)
                .hasMessage("Inspection " + inspection.getId()
                        + " has no findings yet; record at least one before completing it");
        assertThatThrownBy(() -> MaintenanceFixtures.pendingInspection().complete(MaintenanceFixtures.NOW))
                .isInstanceOf(MaintenanceStateException.class);
    }

    @Test
    void onlyAPendingInspectionCanBeCancelled() {
        Inspection pending = MaintenanceFixtures.pendingInspection();
        Inspection inProgress = MaintenanceFixtures.inProgressInspection();

        pending.cancel();

        assertThat(pending.getStatus()).isEqualTo(InspectionStatus.CANCELLED);
        assertThatThrownBy(inProgress::cancel).isInstanceOf(MaintenanceStateException.class);
        assertThatThrownBy(() -> pending.start(MaintenanceFixtures.NOW)).isInstanceOf(MaintenanceStateException.class);
    }
}
