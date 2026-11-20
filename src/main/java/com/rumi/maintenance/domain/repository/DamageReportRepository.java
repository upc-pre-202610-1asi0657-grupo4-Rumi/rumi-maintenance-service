package com.rumi.maintenance.domain.repository;

import com.rumi.maintenance.domain.model.DamageReport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DamageReportRepository {

    Optional<DamageReport> findById(UUID id);

    /**
     * Damage reports of a building, newest first. When {@code includeResolved} is false, resolved reports
     * are left out.
     */
    List<DamageReport> findByBuildingId(UUID buildingId, boolean includeResolved);

    DamageReport save(DamageReport report);
}
