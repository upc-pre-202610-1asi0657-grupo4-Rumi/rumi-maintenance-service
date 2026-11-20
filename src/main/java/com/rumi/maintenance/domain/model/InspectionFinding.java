package com.rumi.maintenance.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Something the engineer found during an inspection, with its notes, severity and photo (US18). Findings
 * only exist inside their inspection.
 */
public final class InspectionFinding {

    public static final int MAX_NOTES_LENGTH = 500;

    private final UUID id;
    private final String notes;
    private final FindingSeverity severity;
    private final PhotoEvidence evidence;
    private final Instant recordedAt;

    public InspectionFinding(UUID id, String notes, FindingSeverity severity, PhotoEvidence evidence,
                             Instant recordedAt) {
        this.id = Objects.requireNonNull(id, "Finding id is required");
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Finding notes are required");
        }
        if (notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("Finding notes must have at most " + MAX_NOTES_LENGTH + " characters");
        }
        this.notes = notes;
        this.severity = Objects.requireNonNull(severity, "Finding severity is required");
        this.evidence = Objects.requireNonNull(evidence, "Photo evidence of the finding is required");
        this.recordedAt = Objects.requireNonNull(recordedAt, "Recording time is required");
    }

    public UUID getId() {
        return id;
    }

    public String getNotes() {
        return notes;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public PhotoEvidence getEvidence() {
        return evidence;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
