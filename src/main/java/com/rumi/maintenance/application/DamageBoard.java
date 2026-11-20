package com.rumi.maintenance.application;

import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.FindingSeverity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Damage board of a building (US22): its damage reports grouped by zone and, inside each zone, by
 * severity. Zones with the most severe damage come first. Reports that the administrator has not assessed
 * yet are grouped apart, with a null severity, after the assessed ones.
 */
public record DamageBoard(UUID buildingId, int totalReports, List<Zone> zones) {

    public record Zone(String zone, int totalReports, FindingSeverity highestSeverity, List<SeverityGroup> groups) {
    }

    public record SeverityGroup(FindingSeverity severity, List<DamageReport> reports) {
    }

    private static final List<FindingSeverity> SEVERITIES_MOST_SEVERE_FIRST =
            List.of(FindingSeverity.HIGH, FindingSeverity.MEDIUM, FindingSeverity.LOW);

    private static final Comparator<DamageReport> NEWEST_FIRST =
            Comparator.comparing(DamageReport::getReportedAt).reversed().thenComparing(DamageReport::getId);

    static DamageBoard of(UUID buildingId, List<DamageReport> reports) {
        Map<String, List<DamageReport>> byZone = reports.stream()
                .collect(Collectors.groupingBy(DamageReport::getZone, TreeMap::new, Collectors.toList()));
        List<Zone> zones = byZone.entrySet().stream()
                .map(entry -> zone(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingInt((Zone zone) -> rank(zone.highestSeverity())).reversed()
                        .thenComparing(Comparator.comparingInt(Zone::totalReports).reversed())
                        .thenComparing(Zone::zone))
                .toList();
        return new DamageBoard(buildingId, reports.size(), zones);
    }

    private static Zone zone(String name, List<DamageReport> reports) {
        List<SeverityGroup> groups = new ArrayList<>();
        FindingSeverity highest = null;
        for (FindingSeverity severity : SEVERITIES_MOST_SEVERE_FIRST) {
            List<DamageReport> ofSeverity = reportsWith(reports, severity);
            if (!ofSeverity.isEmpty()) {
                groups.add(new SeverityGroup(severity, ofSeverity));
                if (highest == null) {
                    highest = severity;
                }
            }
        }
        List<DamageReport> unassessed = reportsWith(reports, null);
        if (!unassessed.isEmpty()) {
            groups.add(new SeverityGroup(null, unassessed));
        }
        return new Zone(name, reports.size(), highest, List.copyOf(groups));
    }

    private static List<DamageReport> reportsWith(List<DamageReport> reports, FindingSeverity severity) {
        return reports.stream()
                .filter(report -> report.getSeverity() == severity)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    private static int rank(FindingSeverity severity) {
        return severity == null ? 0 : severity.rank();
    }
}
