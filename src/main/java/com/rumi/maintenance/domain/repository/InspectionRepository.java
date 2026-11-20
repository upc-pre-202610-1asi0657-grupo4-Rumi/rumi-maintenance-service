package com.rumi.maintenance.domain.repository;

import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionRepository {

    Optional<Inspection> findById(UUID id);

    /**
     * Inspections of a building ordered by scheduled date, the calendar of US17. When {@code status} is
     * null, inspections of every status are returned.
     */
    List<Inspection> findByBuildingId(UUID buildingId, InspectionStatus status);

    Inspection save(Inspection inspection);
}
