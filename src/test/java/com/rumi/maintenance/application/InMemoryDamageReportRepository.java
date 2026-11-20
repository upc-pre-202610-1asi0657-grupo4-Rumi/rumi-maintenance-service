package com.rumi.maintenance.application;

import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.repository.DamageReportRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryDamageReportRepository implements DamageReportRepository {

    private final Map<UUID, DamageReport> reports = new LinkedHashMap<>();

    @Override
    public Optional<DamageReport> findById(UUID id) {
        return Optional.ofNullable(reports.get(id));
    }

    @Override
    public List<DamageReport> findByBuildingId(UUID buildingId, boolean includeResolved) {
        return reports.values().stream()
                .filter(report -> report.belongsTo(buildingId))
                .filter(report -> includeResolved || report.isOpen())
                .sorted(Comparator.comparing(DamageReport::getReportedAt).reversed())
                .toList();
    }

    @Override
    public DamageReport save(DamageReport report) {
        reports.put(report.getId(), report);
        return report;
    }

    List<DamageReport> saved() {
        return List.copyOf(reports.values());
    }
}
