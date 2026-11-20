package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.domain.model.DamageReportStatus;
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
@Table(name = "damage_reports")
public class DamageReportJpaEntity {

    @Id
    private UUID id;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "resident_user_id")
    private UUID residentUserId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String zone;

    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    @Column(name = "photo_taken_at")
    private Instant photoTakenAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FindingSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DamageReportStatus status;

    @Column(name = "generated_inspection_id")
    private UUID generatedInspectionId;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected DamageReportJpaEntity() {
    }

    @SuppressWarnings("java:S107")
    public DamageReportJpaEntity(UUID id, UUID buildingId, UUID residentUserId, String description, String zone,
                                 String photoUrl, Instant photoTakenAt, FindingSeverity severity,
                                 DamageReportStatus status, UUID generatedInspectionId, Instant reportedAt,
                                 Instant reviewedAt, Instant resolvedAt) {
        this.id = id;
        this.buildingId = buildingId;
        this.residentUserId = residentUserId;
        this.description = description;
        this.zone = zone;
        this.photoUrl = photoUrl;
        this.photoTakenAt = photoTakenAt;
        this.severity = severity;
        this.status = status;
        this.generatedInspectionId = generatedInspectionId;
        this.reportedAt = reportedAt;
        this.reviewedAt = reviewedAt;
        this.resolvedAt = resolvedAt;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Instant getPhotoTakenAt() {
        return photoTakenAt;
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
