package com.rumi.maintenance.domain.model;

import com.rumi.maintenance.MaintenanceFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DamageReportTest {

    @Test
    void aReportedDamageIsVisibleWithItsPhotoZoneAndReporterButWithoutSeverity() {
        DamageReport report = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");

        assertThat(report.getStatus()).isEqualTo(DamageReportStatus.REPORTED);
        assertThat(report.getBuildingId()).isEqualTo(MaintenanceFixtures.BUILDING_ID);
        assertThat(report.getResidentUserId()).isEqualTo(MaintenanceFixtures.RESIDENT_ID);
        assertThat(report.getZone()).isEqualTo("FLOOR-2-EAST");
        assertThat(report.getEvidence().url()).isEqualTo(MaintenanceFixtures.PHOTO_URL);
        assertThat(report.getReportedAt()).isEqualTo(MaintenanceFixtures.NOW);
        assertThat(report.getSeverity()).isNull();
        assertThat(report.isOpen()).isTrue();
    }

    @Test
    void aReportNeedsADescriptionAZoneAndAPhoto() {
        assertThatThrownBy(() -> DamageReport.report(UUID.randomUUID(), MaintenanceFixtures.BUILDING_ID, null, "",
                "FLOOR-2-EAST", MaintenanceFixtures.photo(), MaintenanceFixtures.NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Damage description is required");
        assertThatThrownBy(() -> DamageReport.report(UUID.randomUUID(), MaintenanceFixtures.BUILDING_ID, null,
                "Crack", "z".repeat(51), MaintenanceFixtures.photo(), MaintenanceFixtures.NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zone must have at most 50 characters");
        assertThatThrownBy(() -> DamageReport.report(UUID.randomUUID(), MaintenanceFixtures.BUILDING_ID, null,
                "Crack", "FLOOR-2-EAST", null, MaintenanceFixtures.NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Photo of the damage is required");
    }

    @Test
    void theAdministratorAssignsTheSeverityAndKeepsTheReportUnderReview() {
        DamageReport report = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");
        Instant at = MaintenanceFixtures.NOW.plusSeconds(300);

        report.review(FindingSeverity.HIGH, at);
        report.review(FindingSeverity.MEDIUM, at.plusSeconds(60));

        assertThat(report.getStatus()).isEqualTo(DamageReportStatus.UNDER_REVIEW);
        assertThat(report.getSeverity()).isEqualTo(FindingSeverity.MEDIUM);
        assertThat(report.getReviewedAt()).isEqualTo(at.plusSeconds(60));
    }

    @Test
    void schedulingAnInspectionLinksItToTheReportOnlyOnce() {
        DamageReport report = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");
        UUID inspectionId = UUID.randomUUID();

        report.linkInspection(inspectionId, MaintenanceFixtures.NOW);

        assertThat(report.getGeneratedInspectionId()).isEqualTo(inspectionId);
        assertThat(report.getStatus()).isEqualTo(DamageReportStatus.UNDER_REVIEW);
        assertThat(report.getReviewedAt()).isEqualTo(MaintenanceFixtures.NOW);
        assertThatThrownBy(() -> report.linkInspection(UUID.randomUUID(), MaintenanceFixtures.NOW))
                .isInstanceOf(MaintenanceStateException.class)
                .hasMessage("Damage report " + report.getId() + " is already linked to inspection " + inspectionId);
    }

    @Test
    void aReportIsResolvedWithASeverityAndThenCannotChange() {
        DamageReport report = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");
        Instant at = MaintenanceFixtures.NOW.plusSeconds(600);

        report.resolve(FindingSeverity.LOW, at);

        assertThat(report.getStatus()).isEqualTo(DamageReportStatus.RESOLVED);
        assertThat(report.getSeverity()).isEqualTo(FindingSeverity.LOW);
        assertThat(report.getResolvedAt()).isEqualTo(at);
        assertThat(report.isOpen()).isFalse();
        assertThatThrownBy(() -> report.review(FindingSeverity.HIGH, at))
                .isInstanceOf(MaintenanceStateException.class)
                .hasMessage("Damage report " + report.getId() + " is already RESOLVED and cannot be reviewed");
        assertThatThrownBy(() -> report.resolve(FindingSeverity.HIGH, at))
                .isInstanceOf(MaintenanceStateException.class);
    }

    @Test
    void aReportWithoutSeverityCannotBeResolved() {
        DamageReport report = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");

        assertThatThrownBy(() -> report.resolve(null, MaintenanceFixtures.NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Assign a severity before resolving damage report " + report.getId());
        assertThat(report.getStatus()).isEqualTo(DamageReportStatus.REPORTED);
    }
}
