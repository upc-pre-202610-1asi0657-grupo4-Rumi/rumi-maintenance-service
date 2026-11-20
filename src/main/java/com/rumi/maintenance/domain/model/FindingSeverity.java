package com.rumi.maintenance.domain.model;

/**
 * Severity of an inspection finding or of a damage report, assigned by the administrator.
 */
public enum FindingSeverity {
    LOW,
    MEDIUM,
    HIGH;

    /**
     * Higher numbers are more severe, so the damage board can show the worst damage first.
     */
    public int rank() {
        return ordinal() + 1;
    }
}
