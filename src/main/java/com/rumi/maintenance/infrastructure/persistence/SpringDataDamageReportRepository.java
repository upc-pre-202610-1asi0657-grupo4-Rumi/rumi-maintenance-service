package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.domain.model.DamageReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDamageReportRepository extends JpaRepository<DamageReportJpaEntity, UUID> {

    List<DamageReportJpaEntity> findByBuildingIdOrderByReportedAtDescIdAsc(UUID buildingId);

    List<DamageReportJpaEntity> findByBuildingIdAndStatusNotOrderByReportedAtDescIdAsc(
            UUID buildingId, DamageReportStatus status);
}
