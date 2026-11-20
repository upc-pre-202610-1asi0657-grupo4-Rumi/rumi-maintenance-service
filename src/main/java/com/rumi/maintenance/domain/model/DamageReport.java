package com.rumi.maintenance.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Visible damage reported by a resident with a photo and the zone where it is (US21). The administrator
 * reviews it on the damage board (US22), assigns its severity, may schedule an inspection for it and
 * finally marks it as RESOLVED.
 */
public final class DamageReport {

    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_ZONE_LENGTH = 50;

    private final UUID id;
    private final UUID buildingId;
    private final UUID residentUserId;
    private final String description;
    private final String zone;
    private final PhotoEvidence evidence;
    private final Instant reportedAt;
    private FindingSeverity severity;
    private DamageReportStatus status;
    private UUID generatedInspectionId;
    private Instant reviewedAt;
    private Instant resolvedAt;

    @SuppressWarnings("java:S107")
    public DamageReport(
            UUID id,
            UUID buildingId,
            UUID residentUserId,
            String description,
            String zone,
            PhotoEvidence evidence,
            FindingSeverity severity,
            DamageReportStatus status,
            UUID generatedInspectionId,
            Instant reportedAt,
            Instant reviewedAt,
            Instant resolvedAt
    ) {
        this.id = Objects.requireNonNull(id, "Damage report id is required");
        this.buildingId = Objects.requireNonNull(buildingId, "Building id is required");
        this.residentUserId = residentUserId;
        this.description = requireText(description, "Damage description", MAX_DESCRIPTION_LENGTH);
        this.zone = requireText(zone, "Zone", MAX_ZONE_LENGTH).trim();
        this.evidence = Objects.requireNonNull(evidence, "Photo of the damage is required");
        this.severity = severity;
        this.status = Objects.requireNonNull(status, "Damage report status is required");
        this.generatedInspectionId = generatedInspectionId;
        this.reportedAt = Objects.requireNonNull(reportedAt, "Report time is required");
        this.reviewedAt = reviewedAt;
        this.resolvedAt = resolvedAt;
    }

    /**
     * A resident reports a visible damage (US21). It starts as REPORTED and without severity: the
     * administrator assigns the severity when reviewing it.
     */
    public static DamageReport report(UUID id, UUID buildingId, UUID residentUserId, String description,
                                      String zone, PhotoEvidence evidence, Instant now) {
        return new DamageReport(id, buildingId, residentUserId, description, zone, evidence, null,
                DamageReportStatus.REPORTED, null, now, null, null);
    }

    /**
     * The administrator assigns the severity of the damage and keeps the report under review.
     */
    public void review(FindingSeverity newSeverity, Instant at) {
        requireNotResolved("be reviewed");
        this.severity = Objects.requireNonNull(newSeverity, "Severity is required");
        this.status = DamageReportStatus.UNDER_REVIEW;
        this.reviewedAt = Objects.requireNonNull(at, "Review time is required");
    }

    /**
     * Links the inspection scheduled to evaluate the damage. A report can generate one inspection only.
     */
    public void linkInspection(UUID inspectionId, Instant at) {
        Objects.requireNonNull(inspectionId, "Inspection id is required");
        requireNotResolved("be linked to an inspection");
        if (generatedInspectionId != null && !generatedInspectionId.equals(inspectionId)) {
            throw new MaintenanceStateException(
                    "Damage report " + id + " is already linked to inspection " + generatedInspectionId);
        }
        this.generatedInspectionId = inspectionId;
        this.status = DamageReportStatus.UNDER_REVIEW;
        if (reviewedAt == null) {
            this.reviewedAt = Objects.requireNonNull(at, "Review time is required");
        }
    }

    /**
     * Closes the report, with or without an inspection. A resolved report keeps the severity it had, or
     * receives the one given now.
     */
    public void resolve(FindingSeverity finalSeverity, Instant at) {
        requireNotResolved("be resolved");
        if (finalSeverity != null) {
            this.severity = finalSeverity;
        }
        if (severity == null) {
            throw new IllegalArgumentException("Assign a severity before resolving damage report " + id);
        }
        this.status = DamageReportStatus.RESOLVED;
        this.resolvedAt = Objects.requireNonNull(at, "Resolution time is required");
        if (reviewedAt == null) {
            this.reviewedAt = at;
        }
    }

    public boolean isOpen() {
        return status != DamageReportStatus.RESOLVED;
    }

    public boolean belongsTo(UUID otherBuildingId) {
        return buildingId.equals(otherBuildingId);
    }

    private void requireNotResolved(String action) {
        if (status == DamageReportStatus.RESOLVED) {
            throw new MaintenanceStateException("Damage report " + id + " is already RESOLVED and cannot " + action);
        }
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must have at most " + maxLength + " characters");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBuildingId() {
        return buildingId;
    }

    public UUID getResidentUserId() {
        return residentUserId;
    }

    public String getDescription() {
        return description;
    }

    public String getZone() {
        return zone;
    }

    public PhotoEvidence getEvidence() {
        return evidence;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public DamageReportStatus getStatus() {
        return status;
    }

    public UUID getGeneratedInspectionId() {
        return generatedInspectionId;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
