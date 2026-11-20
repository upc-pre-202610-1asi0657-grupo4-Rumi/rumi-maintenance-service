package com.rumi.maintenance.application;

import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionStatus;
import com.rumi.maintenance.domain.repository.InspectionRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryInspectionRepository implements InspectionRepository {

    private final Map<UUID, Inspection> inspections = new LinkedHashMap<>();

    @Override
    public Optional<Inspection> findById(UUID id) {
        return Optional.ofNullable(inspections.get(id));
    }

    @Override
    public List<Inspection> findByBuildingId(UUID buildingId, InspectionStatus status) {
        return inspections.values().stream()
                .filter(inspection -> inspection.belongsTo(buildingId))
                .filter(inspection -> status == null || inspection.getStatus() == status)
                .sorted(Comparator.comparing(Inspection::getScheduledDate))
                .toList();
    }

    @Override
    public Inspection save(Inspection inspection) {
        inspections.put(inspection.getId(), inspection);
        return inspection;
    }

    List<Inspection> saved() {
        return List.copyOf(inspections.values());
    }
}
