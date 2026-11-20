package com.rumi.maintenance.domain.model;

/**
 * Life cycle of an inspection: it is scheduled as PENDING, the engineer starts the visit (IN_PROGRESS),
 * and it is COMPLETED once its findings are recorded. A PENDING inspection can also be CANCELLED.
 */
public enum InspectionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
