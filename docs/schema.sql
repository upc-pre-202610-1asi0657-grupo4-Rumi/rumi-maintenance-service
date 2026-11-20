-- Schema of maintenancedb (PostgreSQL), the database of rumi-maintenance-service.
-- The default profile runs Hibernate with ddl-auto=validate, so these tables must exist.
-- Create the database first:  CREATE DATABASE maintenancedb;
-- Buildings and users belong to other services: they are referenced by identifier only, without
-- foreign keys (one database per service). Photos are stored as URLs, not as files.

CREATE TABLE inspections (
    id             uuid         PRIMARY KEY,
    building_id    uuid         NOT NULL,
    engineer_name  varchar(120) NOT NULL,
    scheduled_date date         NOT NULL,
    status         varchar(20)  NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at     timestamp    NOT NULL,
    started_at     timestamp,
    completed_at   timestamp
);

CREATE TABLE inspection_findings (
    id             uuid         PRIMARY KEY,
    inspection_id  uuid         NOT NULL REFERENCES inspections (id) ON DELETE CASCADE,
    notes          varchar(500) NOT NULL,
    severity       varchar(20)  NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    photo_url      varchar(500) NOT NULL,
    photo_taken_at timestamp,
    recorded_at    timestamp    NOT NULL
);

CREATE TABLE damage_reports (
    id                      uuid         PRIMARY KEY,
    building_id             uuid         NOT NULL,
    resident_user_id        uuid,
    description             varchar(500) NOT NULL,
    zone                    varchar(50)  NOT NULL,
    photo_url               varchar(500) NOT NULL,
    photo_taken_at          timestamp,
    severity                varchar(20)  CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    status                  varchar(20)  NOT NULL CHECK (status IN ('REPORTED', 'UNDER_REVIEW', 'RESOLVED')),
    generated_inspection_id uuid         REFERENCES inspections (id),
    reported_at             timestamp    NOT NULL,
    reviewed_at             timestamp,
    resolved_at             timestamp
);

CREATE INDEX inspections_building_id_scheduled_date_idx ON inspections (building_id, scheduled_date);
CREATE INDEX inspection_findings_inspection_id_idx ON inspection_findings (inspection_id);
CREATE INDEX damage_reports_building_id_reported_at_idx ON damage_reports (building_id, reported_at DESC);
