package com.rumi.maintenance.application;

import java.util.UUID;

public class DamageReportNotFoundException extends ResourceNotFoundException {

    public DamageReportNotFoundException(UUID reportId) {
        super("Damage report " + reportId + " was not found");
    }
}
