package com.rumi.maintenance.application;

import java.util.UUID;

public class InspectionNotFoundException extends ResourceNotFoundException {

    public InspectionNotFoundException(UUID inspectionId) {
        super("Inspection " + inspectionId + " was not found");
    }
}
