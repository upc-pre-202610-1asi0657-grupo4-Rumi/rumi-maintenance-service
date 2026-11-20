package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.MaintenanceFixtures;
import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.FindingSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaDamageReportRepository.class)
class JpaDamageReportRepositoryTest {

    @Autowired
    private JpaDamageReportRepository damageReportRepository;

    @Autowired
    private TestEntityManager entityManager;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void savesAndReadsBackAReportedDamage() {
        DamageReport report = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");

        damageReportRepository.save(report);
        flushAndClear();

        DamageReport stored = damageReportRepository.findById(report.getId()).orElseThrow();
        assertThat(stored.getBuildingId()).isEqualTo(MaintenanceFixtures.BUILDING_ID);
        assertThat(stored.getResidentUserId()).isEqualTo(MaintenanceFixtures.RESIDENT_ID);
        assertThat(stored.getZone()).isEqualTo("FLOOR-2-EAST");
        assertThat(stored.getEvidence().url()).isEqualTo(MaintenanceFixtures.PHOTO_URL);
        assertThat(stored.getStatus()).isEqualTo(DamageReportStatus.REPORTED);
        assertThat(stored.getSeverity()).isNull();
        assertThat(stored.getReportedAt()).isEqualTo(MaintenanceFixtures.NOW);
    }

    @Test
    void storesTheReviewAndTheLinkedInspection() {
        DamageReport report = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");
        UUID inspectionId = UUID.randomUUID();
        report.review(FindingSeverity.HIGH, MaintenanceFixtures.NOW.plusSeconds(60));
        report.linkInspection(inspectionId, MaintenanceFixtures.NOW.plusSeconds(120));

        damageReportRepository.save(report);
        flushAndClear();

        DamageReport stored = damageReportRepository.findById(report.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(DamageReportStatus.UNDER_REVIEW);
        assertThat(stored.getSeverity()).isEqualTo(FindingSeverity.HIGH);
        assertThat(stored.getGeneratedInspectionId()).isEqualTo(inspectionId);
        assertThat(stored.getReviewedAt()).isEqualTo(MaintenanceFixtures.NOW.plusSeconds(60));
    }

    @Test
    void listsTheReportsOfABuildingNewestFirstAndCanLeaveOutTheResolvedOnes() {
        DamageReport older = MaintenanceFixtures.reportedDamage(MaintenanceFixtures.BUILDING_ID, "LOBBY",
                MaintenanceFixtures.NOW.minusSeconds(3600));
        DamageReport newer = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");
        DamageReport resolved = MaintenanceFixtures.reportedDamage(MaintenanceFixtures.BUILDING_ID, "STAIRWELL-B",
                MaintenanceFixtures.NOW.minusSeconds(60));
        resolved.resolve(FindingSeverity.LOW, MaintenanceFixtures.NOW);
        DamageReport otherBuilding = MaintenanceFixtures.reportedDamage(MaintenanceFixtures.OTHER_BUILDING_ID,
                "LOBBY", MaintenanceFixtures.NOW);
        damageReportRepository.save(older);
        damageReportRepository.save(newer);
        damageReportRepository.save(resolved);
        damageReportRepository.save(otherBuilding);
        flushAndClear();

        assertThat(damageReportRepository.findByBuildingId(MaintenanceFixtures.BUILDING_ID, false).stream()
                .map(DamageReport::getId).toList())
                .containsExactly(newer.getId(), older.getId());
        assertThat(damageReportRepository.findByBuildingId(MaintenanceFixtures.BUILDING_ID, true).stream()
                .map(DamageReport::getId).toList())
                .containsExactly(newer.getId(), resolved.getId(), older.getId());
    }
}
