package com.rumi.maintenance.application;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data to schedule an inspection (US17). {@code damageReportId} is optional: when present, the inspection
 * is scheduled to evaluate that damage report and the report is linked to it.
 */
public record ScheduleInspectionCommand(
        UUID buildingId,
        String engineerName,
        LocalDate scheduledDate,
        UUID damageReportId
) {
}
