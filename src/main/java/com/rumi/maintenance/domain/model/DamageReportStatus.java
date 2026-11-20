package com.rumi.maintenance.domain.model;

/**
 * Life cycle of a damage report: a resident REPORTED it, the administrator puts it UNDER_REVIEW (for
 * example, by scheduling an inspection) and finally marks it RESOLVED.
 */
public enum DamageReportStatus {
    REPORTED,
    UNDER_REVIEW,
    RESOLVED
}
