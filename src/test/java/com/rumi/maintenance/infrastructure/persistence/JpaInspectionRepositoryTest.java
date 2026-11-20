package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.MaintenanceFixtures;
import com.rumi.maintenance.domain.model.FindingSeverity;
import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionFinding;
import com.rumi.maintenance.domain.model.InspectionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaInspectionRepository.class)
class JpaInspectionRepositoryTest {

    @Autowired
    private JpaInspectionRepository inspectionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void savesAndReadsBackAPendingInspection() {
        Inspection inspection = MaintenanceFixtures.pendingInspection();

        inspectionRepository.save(inspection);
        flushAndClear();

        Inspection stored = inspectionRepository.findById(inspection.getId()).orElseThrow();
        assertThat(stored.getBuildingId()).isEqualTo(MaintenanceFixtures.BUILDING_ID);
        assertThat(stored.getEngineerName()).isEqualTo("Ing. Carla Mendoza");
        assertThat(stored.getScheduledDate()).isEqualTo(MaintenanceFixtures.NEXT_WEEK);
        assertThat(stored.getStatus()).isEqualTo(InspectionStatus.PENDING);
        assertThat(stored.getCreatedAt()).isEqualTo(MaintenanceFixtures.NOW);
        assertThat(stored.getFindings()).isEmpty();
    }

    @Test
    void storesTheFindingsOfAnInspectionInTheOrderTheyWereRecorded() {
        Inspection inspection = MaintenanceFixtures.inspectionWithFinding(FindingSeverity.MEDIUM);
        inspectionRepository.save(inspection);
        flushAndClear();

        Inspection reloaded = inspectionRepository.findById(inspection.getId()).orElseThrow();
        reloaded.recordFinding(UUID.randomUUID(), "Spalled concrete in column C-4", FindingSeverity.HIGH,
                MaintenanceFixtures.photo(), MaintenanceFixtures.NOW.plusSeconds(600));
        inspectionRepository.save(reloaded);
        flushAndClear();

        Inspection stored = inspectionRepository.findById(inspection.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(InspectionStatus.IN_PROGRESS);
        assertThat(stored.getFindings()).hasSize(2);
        InspectionFinding first = stored.getFindings().get(0);
        assertThat(first.getSeverity()).isEqualTo(FindingSeverity.MEDIUM);
        assertThat(first.getEvidence().url()).isEqualTo(MaintenanceFixtures.PHOTO_URL);
        assertThat(first.getEvidence().takenAt()).isEqualTo(MaintenanceFixtures.NOW.minusSeconds(600));
        assertThat(stored.getFindings().get(1).getNotes()).isEqualTo("Spalled concrete in column C-4");
        assertThat(stored.highestSeverity()).isEqualTo(FindingSeverity.HIGH);
    }

    @Test
    void storesTheCompletionOfAnInspection() {
        Inspection inspection = MaintenanceFixtures.inspectionWithFinding(FindingSeverity.LOW);
        inspection.complete(MaintenanceFixtures.NOW.plusSeconds(3600));

        inspectionRepository.save(inspection);
        flushAndClear();

        Inspection stored = inspectionRepository.findById(inspection.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(InspectionStatus.COMPLETED);
        assertThat(stored.getStartedAt()).isEqualTo(MaintenanceFixtures.NOW.plusSeconds(60));
        assertThat(stored.getCompletedAt()).isEqualTo(MaintenanceFixtures.NOW.plusSeconds(3600));
    }

    @Test
    void listsTheCalendarOfABuildingByDateAndFiltersByStatus() {
        Inspection later = Inspection.schedule(UUID.randomUUID(), MaintenanceFixtures.BUILDING_ID, "Ing. Ana Ruiz",
                LocalDate.parse("2026-12-15"), MaintenanceFixtures.TODAY, MaintenanceFixtures.NOW);
        Inspection sooner = MaintenanceFixtures.inProgressInspection();
        Inspection otherBuilding = Inspection.schedule(UUID.randomUUID(), MaintenanceFixtures.OTHER_BUILDING_ID,
                "Ing. Ana Ruiz", MaintenanceFixtures.NEXT_WEEK, MaintenanceFixtures.TODAY, MaintenanceFixtures.NOW);
        inspectionRepository.save(later);
        inspectionRepository.save(sooner);
        inspectionRepository.save(otherBuilding);
        flushAndClear();

        assertThat(inspectionRepository.findByBuildingId(MaintenanceFixtures.BUILDING_ID, null).stream()
                .map(Inspection::getId).toList())
                .containsExactly(sooner.getId(), later.getId());
        assertThat(inspectionRepository.findByBuildingId(MaintenanceFixtures.BUILDING_ID, InspectionStatus.PENDING)
                .stream().map(Inspection::getId).toList())
                .containsExactly(later.getId());
        assertThat(inspectionRepository.findById(UUID.randomUUID())).isEmpty();
    }
}
