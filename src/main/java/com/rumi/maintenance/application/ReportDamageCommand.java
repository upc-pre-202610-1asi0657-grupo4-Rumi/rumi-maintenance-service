package com.rumi.maintenance.application;

import java.time.Instant;
import java.util.UUID;

/**
 * A damage reported by a resident (US21). {@code residentUserId} comes from the X-User-Id header of the
 * gateway and is null when the request did not go through it.
 */
public record ReportDamageCommand(
        UUID buildingId,
        UUID residentUserId,
        String description,
        String zone,
        String photoUrl,
        Instant photoTakenAt
) {
}
