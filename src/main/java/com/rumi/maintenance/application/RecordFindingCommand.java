package com.rumi.maintenance.application;

import com.rumi.maintenance.domain.model.FindingSeverity;

import java.time.Instant;
import java.util.UUID;

/**
 * A finding of an inspection in progress (US18), with the address of its photo.
 */
public record RecordFindingCommand(
        UUID inspectionId,
        String notes,
        FindingSeverity severity,
        String photoUrl,
        Instant photoTakenAt
) {
}
