package com.rumi.maintenance.application;

import com.rumi.maintenance.MaintenanceFixtures;
import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.FindingSeverity;
import com.rumi.maintenance.domain.model.MaintenanceStateException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DamageReportApplicationServiceTest {

    private static final UUID BUILDING_ID = MaintenanceFixtures.BUILDING_ID;
    private static final Instant NOW = MaintenanceFixtures.NOW;

    private final InMemoryDamageReportRepository repository = new InMemoryDamageReportRepository();
    private final DamageReportApplicationService service = new DamageReportApplicationService(
            repository, Clock.fixed(NOW, ZoneId.of("America/Lima")));

    private ReportDamageCommand report(String zone) {
        return new ReportDamageCommand(BUILDING_ID, MaintenanceFixtures.RESIDENT_ID,
                "Crack in the wall next to the elevator", zone, MaintenanceFixtures.PHOTO_URL, null);
    }

    @Test
    void registersTheDamageReportedByAResident() {
        DamageReport report = service.reportDamage(report("FLOOR-2-EAST"));

        assertThat(report.getStatus()).isEqualTo(DamageReportStatus.REPORTED);
        assertThat(report.getResidentUserId()).isEqualTo(MaintenanceFixtures.RESIDENT_ID);
        assertThat(report.getReportedAt()).isEqualTo(NOW);
        assertThat(repository.saved()).containsExactly(report);
    }

    @Test
    void rejectsAReportWithoutPhotoOrBuilding() {
        assertThatThrownBy(() -> service.reportDamage(new ReportDamageCommand(BUILDING_ID, null, "Crack",
                "FLOOR-2-EAST", "", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Photo URL is required");
        assertThatThrownBy(() -> service.reportDamage(new ReportDamageCommand(null, null, "Crack",
                "FLOOR-2-EAST", MaintenanceFixtures.PHOTO_URL, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Building id is required");
        assertThat(repository.saved()).isEmpty();
    }

    @Test
    void theBoardGroupsTheReportsByZoneAndSeverityWithTheMostSevereZoneFirst() {
        DamageReport stairsMedium = repository.save(MaintenanceFixtures.reviewedDamage(
                "STAIRWELL-B", FindingSeverity.MEDIUM, NOW.minusSeconds(3000)));
        DamageReport floorUnassessed = repository.save(MaintenanceFixtures.reportedDamage(
                BUILDING_ID, "FLOOR-2-EAST", NOW.minusSeconds(1000)));
        DamageReport floorHigh = repository.save(MaintenanceFixtures.reviewedDamage(
                "FLOOR-2-EAST", FindingSeverity.HIGH, NOW.minusSeconds(2000)));
        DamageReport lobbyLow = repository.save(MaintenanceFixtures.reviewedDamage(
                "LOBBY", FindingSeverity.LOW, NOW.minusSeconds(500)));
        repository.save(MaintenanceFixtures.reportedDamage(MaintenanceFixtures.OTHER_BUILDING_ID, "LOBBY", NOW));

        DamageBoard board = service.getBoard(BUILDING_ID, false);

        assertThat(board.totalReports()).isEqualTo(4);
        assertThat(board.zones()).hasSize(3);
        DamageBoard.Zone floor = board.zones().get(0);
        assertThat(floor.zone()).isEqualTo("FLOOR-2-EAST");
        assertThat(floor.totalReports()).isEqualTo(2);
        assertThat(floor.highestSeverity()).isEqualTo(FindingSeverity.HIGH);
        assertThat(floor.groups()).hasSize(2);
        assertThat(floor.groups().get(0).severity()).isEqualTo(FindingSeverity.HIGH);
        assertThat(floor.groups().get(0).reports()).containsExactly(floorHigh);
        assertThat(floor.groups().get(1).severity()).isNull();
        assertThat(floor.groups().get(1).reports()).containsExactly(floorUnassessed);
        assertThat(board.zones().get(1).zone()).isEqualTo("STAIRWELL-B");
        assertThat(board.zones().get(1).groups().get(0).reports()).containsExactly(stairsMedium);
        assertThat(board.zones().get(2).zone()).isEqualTo("LOBBY");
        assertThat(board.zones().get(2).groups().get(0).reports()).containsExactly(lobbyLow);
    }

    @Test
    void theBoardLeavesOutResolvedReportsUnlessAskedFor() {
        DamageReport resolved = MaintenanceFixtures.reportedDamage("LOBBY");
        resolved.resolve(FindingSeverity.LOW, NOW);
        repository.save(resolved);
        repository.save(MaintenanceFixtures.reportedDamage("FLOOR-2-EAST"));

        assertThat(service.getBoard(BUILDING_ID, false).totalReports()).isEqualTo(1);
        assertThat(service.getBoard(BUILDING_ID, true).totalReports()).isEqualTo(2);
        assertThat(service.getBoard(UUID.randomUUID(), false).zones()).isEmpty();
    }

    @Test
    void theAdministratorReviewsAndThenResolvesAReport() {
        UUID reportId = service.reportDamage(report("FLOOR-2-EAST")).getId();

        DamageReport reviewed = service.reviewDamageReport(
                new ReviewDamageReportCommand(reportId, FindingSeverity.HIGH, DamageReportStatus.UNDER_REVIEW));

        assertThat(reviewed.getSeverity()).isEqualTo(FindingSeverity.HIGH);
        assertThat(reviewed.getStatus()).isEqualTo(DamageReportStatus.UNDER_REVIEW);

        DamageReport resolved = service.reviewDamageReport(
                new ReviewDamageReportCommand(reportId, FindingSeverity.MEDIUM, DamageReportStatus.RESOLVED));

        assertThat(resolved.getStatus()).isEqualTo(DamageReportStatus.RESOLVED);
        assertThat(resolved.getSeverity()).isEqualTo(FindingSeverity.MEDIUM);
        assertThat(resolved.getResolvedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> service.reviewDamageReport(
                new ReviewDamageReportCommand(reportId, FindingSeverity.LOW, DamageReportStatus.UNDER_REVIEW)))
                .isInstanceOf(MaintenanceStateException.class);
    }

    @Test
    void aReviewNeedsASeverityAndCannotGoBackToReported() {
        UUID reportId = service.reportDamage(report("FLOOR-2-EAST")).getId();

        assertThatThrownBy(() -> service.reviewDamageReport(
                new ReviewDamageReportCommand(reportId, null, DamageReportStatus.UNDER_REVIEW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Severity is required");
        assertThatThrownBy(() -> service.reviewDamageReport(
                new ReviewDamageReportCommand(reportId, FindingSeverity.LOW, DamageReportStatus.REPORTED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A reviewed damage report can only be UNDER_REVIEW or RESOLVED");
        assertThatThrownBy(() -> service.reviewDamageReport(new ReviewDamageReportCommand(
                UUID.randomUUID(), FindingSeverity.LOW, DamageReportStatus.UNDER_REVIEW)))
                .isInstanceOf(DamageReportNotFoundException.class);
    }
}
