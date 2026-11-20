package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.domain.model.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataInspectionRepository extends JpaRepository<InspectionJpaEntity, UUID> {

    List<InspectionJpaEntity> findByBuildingIdOrderByScheduledDateAscCreatedAtAsc(UUID buildingId);

    List<InspectionJpaEntity> findByBuildingIdAndStatusOrderByScheduledDateAscCreatedAtAsc(
            UUID buildingId, InspectionStatus status);
}
