package com.rumi.maintenance;

import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.FindingSeverity;
import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.PhotoEvidence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class MaintenanceFixtures {

    public static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    public static final UUID OTHER_BUILDING_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
    public static final UUID RESIDENT_ID = UUID.fromString("d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70");
    public static final Instant NOW = Instant.parse("2026-11-20T16:00:00Z");
    public static final LocalDate TODAY = LocalDate.parse("2026-11-20");
    public static final LocalDate NEXT_WEEK = LocalDate.parse("2026-11-27");
    public static final String PHOTO_URL = "https://storage.example.com/rumi/damage/floor2-wall.jpg";

    private MaintenanceFixtures() {
    }

    public static PhotoEvidence photo() {
        return new PhotoEvidence(PHOTO_URL, NOW.minusSeconds(600));
    }

    public static Inspection pendingInspection() {
        return Inspection.schedule(UUID.randomUUID(), BUILDING_ID, "Ing. Carla Mendoza", NEXT_WEEK, TODAY, NOW);
    }

    public static Inspection inProgressInspection() {
        Inspection inspection = pendingInspection();
        inspection.start(NOW.plusSeconds(60));
        return inspection;
    }

    public static Inspection inspectionWithFinding(FindingSeverity severity) {
        Inspection inspection = inProgressInspection();
        inspection.recordFinding(UUID.randomUUID(), "Diagonal crack of 3 mm in the beam of axis B-2", severity,
                photo(), NOW.plusSeconds(120));
        return inspection;
    }

    public static DamageReport reportedDamage(String zone) {
        return reportedDamage(BUILDING_ID, zone, NOW);
    }

    public static DamageReport reportedDamage(UUID buildingId, String zone, Instant reportedAt) {
        return DamageReport.report(UUID.randomUUID(), buildingId, RESIDENT_ID,
                "Crack in the wall next to the elevator", zone, photo(), reportedAt);
    }

    public static DamageReport reviewedDamage(String zone, FindingSeverity severity, Instant reportedAt) {
        DamageReport report = reportedDamage(BUILDING_ID, zone, reportedAt);
        report.review(severity, reportedAt.plusSeconds(60));
        return report;
    }
}
