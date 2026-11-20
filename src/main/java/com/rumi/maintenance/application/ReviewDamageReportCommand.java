package com.rumi.maintenance.application;

import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.FindingSeverity;

import java.util.UUID;

/**
 * Decision of the administrator about a damage report: its severity and whether it stays UNDER_REVIEW or
 * is RESOLVED.
 */
public record ReviewDamageReportCommand(
        UUID reportId,
        FindingSeverity severity,
        DamageReportStatus status
) {
}
