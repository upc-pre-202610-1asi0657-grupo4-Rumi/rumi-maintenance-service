package com.rumi.maintenance.domain.model;

/**
 * An inspection or a damage report was asked to do something that its current status does not allow.
 */
public class MaintenanceStateException extends RuntimeException {

    public MaintenanceStateException(String message) {
        super(message);
    }
}
