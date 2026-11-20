package com.rumi.maintenance.infrastructure.seed;

import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.FindingSeverity;
import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionFinding;
import com.rumi.maintenance.domain.model.InspectionStatus;
import com.rumi.maintenance.domain.model.PhotoEvidence;
import com.rumi.maintenance.domain.repository.DamageReportRepository;
import com.rumi.maintenance.domain.repository.InspectionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Loads sample inspections and damage reports of the sample building when the service runs with the dev
 * profile. The building id is the fixed id of the sample data of rumi-building-service. The pending
 * inspection is always scheduled one week ahead, so it can be started in a demo.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    public static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    public static final UUID RESIDENT_USER_ID = UUID.fromString("d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70");
    public static final UUID PENDING_INSPECTION_ID = UUID.fromString("3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21");
    public static final UUID COMPLETED_INSPECTION_ID = UUID.fromString("6a8c0e2b-4d6f-4a1c-8e3b-5d7f9b1d3f46");
    public static final UUID FINDING_ID = UUID.fromString("5b7d9f1a-3c5e-4a7b-9d1f-3e5a7c9e1b24");
    public static final UUID REPORTED_DAMAGE_ID = UUID.fromString("8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13");
    public static final UUID HIGH_DAMAGE_ID = UUID.fromString("9d3f5b7a-2c4e-4a6b-8d0f-3e5a7b9d1f35");
    public static final UUID MEDIUM_DAMAGE_ID = UUID.fromString("1e4a6c8b-3d5f-4b7c-9e1a-4f6b8d0f2a57");

    private final InspectionRepository inspectionRepository;
    private final DamageReportRepository damageReportRepository;
    private final Clock clock;

    public DevDataSeeder(InspectionRepository inspectionRepository, DamageReportRepository damageReportRepository,
                         Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.damageReportRepository = damageReportRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant tenDaysAgo = Instant.parse("2026-11-10T14:00:00Z");
        inspectionRepository.save(new Inspection(
                COMPLETED_INSPECTION_ID, BUILDING_ID, "Ing. Jorge Salazar", LocalDate.parse("2026-11-12"),
                InspectionStatus.COMPLETED, tenDaysAgo,
                Instant.parse("2026-11-12T15:00:00Z"), Instant.parse("2026-11-12T17:00:00Z"),
                List.of(new InspectionFinding(
                        FINDING_ID,
                        "Spalled concrete with exposed rebar in column C-4 of the parking access",
                        FindingSeverity.HIGH,
                        new PhotoEvidence("https://storage.example.com/rumi/inspections/column-c4.jpg",
                                Instant.parse("2026-11-12T15:40:00Z")),
                        Instant.parse("2026-11-12T15:45:00Z")))));
        inspectionRepository.save(new Inspection(
                PENDING_INSPECTION_ID, BUILDING_ID, "Ing. Carla Mendoza", LocalDate.now(clock).plusDays(7),
                InspectionStatus.PENDING, clock.instant(), null, null, List.of()));

        damageReportRepository.save(new DamageReport(
                HIGH_DAMAGE_ID, BUILDING_ID, RESIDENT_USER_ID,
                "Column with spalled concrete in the parking access", "FLOOR-2-EAST",
                new PhotoEvidence("https://storage.example.com/rumi/damage/floor2-column.jpg", null),
                FindingSeverity.HIGH, DamageReportStatus.UNDER_REVIEW, COMPLETED_INSPECTION_ID,
                tenDaysAgo, Instant.parse("2026-11-10T16:00:00Z"), null));
        damageReportRepository.save(new DamageReport(
                MEDIUM_DAMAGE_ID, BUILDING_ID, RESIDENT_USER_ID,
                "Loose handrail between the third and fourth floor", "STAIRWELL-B",
                new PhotoEvidence("https://storage.example.com/rumi/damage/stairwell-handrail.jpg", null),
                FindingSeverity.MEDIUM, DamageReportStatus.UNDER_REVIEW, null,
                Instant.parse("2026-11-15T09:30:00Z"), Instant.parse("2026-11-15T12:00:00Z"), null));
        damageReportRepository.save(new DamageReport(
                REPORTED_DAMAGE_ID, BUILDING_ID, RESIDENT_USER_ID,
                "Crack in the wall next to the elevator", "FLOOR-2-EAST",
                new PhotoEvidence("https://storage.example.com/rumi/damage/floor2-wall.jpg",
                        Instant.parse("2026-11-20T13:45:00Z")),
                null, DamageReportStatus.REPORTED, null,
                Instant.parse("2026-11-20T13:50:00Z"), null, null));
    }
}
