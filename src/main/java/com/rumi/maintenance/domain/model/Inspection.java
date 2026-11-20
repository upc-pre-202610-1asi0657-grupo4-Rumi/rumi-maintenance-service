package com.rumi.maintenance.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A structural inspection of a building, scheduled by the administrator with a responsible engineer (US17).
 * It moves from PENDING to IN_PROGRESS when the visit starts, collects findings while it is in progress
 * (US18) and ends as COMPLETED. A PENDING inspection can be CANCELLED.
 */
public final class Inspection {

    public static final int MAX_ENGINEER_NAME_LENGTH = 120;

    private final UUID id;
    private final UUID buildingId;
    private final String engineerName;
    private final LocalDate scheduledDate;
    private final Instant createdAt;
    private final List<InspectionFinding> findings;
    private InspectionStatus status;
    private Instant startedAt;
    private Instant completedAt;

    @SuppressWarnings("java:S107")
    public Inspection(
            UUID id,
            UUID buildingId,
            String engineerName,
            LocalDate scheduledDate,
            InspectionStatus status,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt,
            List<InspectionFinding> findings
    ) {
        this.id = Objects.requireNonNull(id, "Inspection id is required");
        this.buildingId = Objects.requireNonNull(buildingId, "Building id is required");
        if (engineerName == null || engineerName.isBlank()) {
            throw new IllegalArgumentException("Engineer name is required");
        }
        if (engineerName.length() > MAX_ENGINEER_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Engineer name must have at most " + MAX_ENGINEER_NAME_LENGTH + " characters");
        }
        this.engineerName = engineerName.trim();
        this.scheduledDate = Objects.requireNonNull(scheduledDate, "Scheduled date is required");
        this.status = Objects.requireNonNull(status, "Inspection status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.findings = new ArrayList<>(findings == null ? List.of() : findings);
        this.findings.sort(Comparator.comparing(InspectionFinding::getRecordedAt));
    }

    /**
     * Schedules a new inspection (US17). It starts as PENDING and cannot be scheduled for a past date.
     */
    public static Inspection schedule(UUID id, UUID buildingId, String engineerName, LocalDate scheduledDate,
                                      LocalDate today, Instant now) {
        Objects.requireNonNull(scheduledDate, "Scheduled date is required");
        if (scheduledDate.isBefore(today)) {
            throw new IllegalArgumentException("Scheduled date cannot be in the past");
        }
        return new Inspection(id, buildingId, engineerName, scheduledDate, InspectionStatus.PENDING, now,
                null, null, List.of());
    }

    /**
     * The engineer starts the visit. Only a PENDING inspection can start.
     */
    public void start(Instant at) {
        requireStatus(InspectionStatus.PENDING, "be started");
        this.status = InspectionStatus.IN_PROGRESS;
        this.startedAt = Objects.requireNonNull(at, "Start time is required");
    }

    /**
     * Records a finding of the visit (US18). Findings can only be recorded while the inspection is in progress.
     */
    public InspectionFinding recordFinding(UUID findingId, String notes, FindingSeverity severity,
                                           PhotoEvidence evidence, Instant at) {
        requireStatus(InspectionStatus.IN_PROGRESS, "receive findings");
        InspectionFinding finding = new InspectionFinding(findingId, notes, severity, evidence, at);
        findings.add(finding);
        return finding;
    }

    /**
     * Ends the visit. The inspection must be in progress and have at least one finding.
     */
    public void complete(Instant at) {
        requireStatus(InspectionStatus.IN_PROGRESS, "be completed");
        if (findings.isEmpty()) {
            throw new MaintenanceStateException(
                    "Inspection " + id + " has no findings yet; record at least one before completing it");
        }
        this.status = InspectionStatus.COMPLETED;
        this.completedAt = Objects.requireNonNull(at, "Completion time is required");
    }

    /**
     * Cancels an inspection that has not started.
     */
    public void cancel() {
        requireStatus(InspectionStatus.PENDING, "be cancelled");
        this.status = InspectionStatus.CANCELLED;
    }

    /**
     * The most severe finding, used to tell the administrator how serious the inspection result is.
     */
    public FindingSeverity highestSeverity() {
        return findings.stream()
                .map(InspectionFinding::getSeverity)
                .max(Comparator.comparingInt(FindingSeverity::rank))
                .orElse(null);
    }

    public boolean belongsTo(UUID otherBuildingId) {
        return buildingId.equals(otherBuildingId);
    }

    private void requireStatus(InspectionStatus expected, String action) {
        if (status != expected) {
            throw new MaintenanceStateException(
                    "Inspection " + id + " is " + status + " and cannot " + action);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getBuildingId() {
        return buildingId;
    }

    public String getEngineerName() {
        return engineerName;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public InspectionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<InspectionFinding> getFindings() {
        return Collections.unmodifiableList(findings);
    }
}
