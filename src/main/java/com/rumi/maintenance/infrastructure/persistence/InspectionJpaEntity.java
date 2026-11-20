package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.domain.model.InspectionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inspections")
public class InspectionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "engineer_name", nullable = false, length = 120)
    private String engineerName;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InspectionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "inspection_id", nullable = false, updatable = false)
    @OrderBy("recordedAt ASC")
    private List<InspectionFindingJpaEntity> findings = new ArrayList<>();

    protected InspectionJpaEntity() {
    }

    @SuppressWarnings("java:S107")
    public InspectionJpaEntity(UUID id, UUID buildingId, String engineerName, LocalDate scheduledDate,
                               InspectionStatus status, Instant createdAt, Instant startedAt, Instant completedAt,
                               List<InspectionFindingJpaEntity> findings) {
        this.id = id;
        this.buildingId = buildingId;
        this.engineerName = engineerName;
        this.scheduledDate = scheduledDate;
        this.status = status;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.findings = new ArrayList<>(findings);
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

    public List<InspectionFindingJpaEntity> getFindings() {
        return findings;
    }
}
