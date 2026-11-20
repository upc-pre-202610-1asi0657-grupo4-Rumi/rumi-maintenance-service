package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.domain.model.FindingSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inspection_findings")
public class InspectionFindingJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingSeverity severity;

    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    @Column(name = "photo_taken_at")
    private Instant photoTakenAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected InspectionFindingJpaEntity() {
    }

    public InspectionFindingJpaEntity(UUID id, String notes, FindingSeverity severity, String photoUrl,
                                      Instant photoTakenAt, Instant recordedAt) {
        this.id = id;
        this.notes = notes;
        this.severity = severity;
        this.photoUrl = photoUrl;
        this.photoTakenAt = photoTakenAt;
        this.recordedAt = recordedAt;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Instant getPhotoTakenAt() {
        return photoTakenAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
