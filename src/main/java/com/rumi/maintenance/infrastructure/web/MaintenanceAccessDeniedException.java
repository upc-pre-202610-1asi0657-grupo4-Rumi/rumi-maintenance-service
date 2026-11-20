package com.rumi.maintenance.infrastructure.web;

/**
 * The user forwarded by the gateway is not allowed to do what they asked: a resident tried to manage
 * inspections or the damage board, or to report damage in a building that is not in their token.
 */
public class MaintenanceAccessDeniedException extends RuntimeException {

    public MaintenanceAccessDeniedException(String message) {
        super(message);
    }
}
