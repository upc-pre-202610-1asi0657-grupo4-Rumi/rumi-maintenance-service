# rumi-maintenance-service

Building Maintenance service of **Rumi**, the structural monitoring platform by Kuntur Labs.

| | |
|---|---|
| Bounded context | Building Maintenance |
| Port | `8088` |
| Gateway routes | `/api/v1/inspections/**`, `/api/v1/damage-reports/**` (any valid token; this service applies the role rules) |
| Base package | `com.rumi.maintenance` |
| Database | `maintenancedb` (PostgreSQL) |

## Purpose

Keeps the maintenance plan of each building:

- **Inspections (US17).** The administrator schedules an inspection with a responsible engineer and a date
  (today or later). It is registered in the calendar of the building as `PENDING`.
- **Findings (US18).** When the engineer starts the visit the inspection is `IN_PROGRESS`, and each finding
  is recorded with notes, severity (`LOW`, `MEDIUM`, `HIGH`), photo and date. An inspection is `COMPLETED`
  once it has at least one finding.
- **Damage reports (US21).** A resident reports a visible damage with a photo and the zone where it is.
  The report starts as `REPORTED`, without severity.
- **Damage board (US22).** The administrator sees the reports grouped by zone and, inside each zone, by
  severity, with the most severe zones first. The administrator assigns the severity, keeps the report
  `UNDER_REVIEW` or marks it `RESOLVED`, and can schedule an inspection for it.

Photos are stored as URLs: the client uploads the image to the file storage and sends its address.

## Endpoints

| Verb | Path | Description | Request | Response | User story |
|---|---|---|---|---|---|
| POST | `/api/v1/inspections` | Schedule an inspection; `damageReportId` is optional | `ScheduleInspectionRequest` | `201` `InspectionResponse` | US17 |
| GET | `/api/v1/inspections?buildingId={id}&status={status}` | Calendar of a building ordered by date; `status` is optional | none | `200` `InspectionResponse[]` | US17, US18 |
| PATCH | `/api/v1/inspections/{inspectionId}/start` | The engineer starts the visit (`PENDING` to `IN_PROGRESS`) | none | `200` `InspectionResponse` | US18 |
| POST | `/api/v1/inspections/{inspectionId}/findings` | Record a finding with notes, severity and photo | `InspectionFindingRequest` | `201` `InspectionResponse` | US18 |
| PATCH | `/api/v1/inspections/{inspectionId}/complete` | Complete an inspection that has findings | none | `200` `InspectionResponse` | US18 |
| POST | `/api/v1/damage-reports` | Report a damage with photo and zone | `DamageReportRequest` | `201` `DamageReportResponse` | US21 |
| GET | `/api/v1/damage-reports/board?buildingId={id}&includeResolved={bool}` | Damage board grouped by zone and severity | none | `200` `DamageBoardResponse` | US22 |
| PATCH | `/api/v1/damage-reports/{reportId}/review` | Assign the severity and keep the report under review or resolve it | `DamageReviewRequest` | `200` `DamageReportResponse` | US22 |

Implemented functional endpoints: 8.

Errors follow RFC 7807 (`application/problem+json`): `400` invalid input (for example a past date or a
photo URL that is not http or https), `403` the role or building of the user does not allow the action,
`404` unknown inspection or damage report, `409` the inspection or report is not in a state that allows
the change.

The skeleton endpoint `GET /api/v1/inspections/health` is still available and is not counted.

### Access rules

The gateway validates the JWT and forwards `X-User-Id`, `X-Role` and `X-Building-Id` (comma-separated).
This service applies the rules of the context:

- Only the administrator manages inspections, the damage board and the review of reports. A `RESIDENT`
  gets `403`.
- A `RESIDENT` can only report damage in the buildings of their token (`403` otherwise). The reporter is
  taken from `X-User-Id`.
- Requests without these headers did not go through the gateway and are not restricted.

### Example

```json
{
  "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
  "totalReports": 2,
  "zones": [
    {
      "zone": "FLOOR-2-EAST",
      "totalReports": 2,
      "highestSeverity": "HIGH",
      "groups": [
        { "severity": "HIGH", "reports": [ { "id": "9d3f5b7a-2c4e-4a6b-8d0f-3e5a7b9d1f35", "status": "UNDER_REVIEW", "...": "..." } ] },
        { "severity": "UNASSESSED", "reports": [ { "id": "8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13", "status": "REPORTED", "...": "..." } ] }
      ]
    }
  ]
}
```

## API documentation

- Swagger UI: <http://localhost:8088/swagger-ui.html>
- OpenAPI spec: <http://localhost:8088/v3/api-docs>
- Exported spec: [`docs/openapi.json`](docs/openapi.json)
- Postman collection: [`docs/postman/rumi-maintenance-service.postman_collection.json`](docs/postman/rumi-maintenance-service.postman_collection.json)

## Run

Requirements: JDK 21 (the Maven wrapper is included; use `mvnw.cmd` on Windows).

With the `dev` profile the service needs no database: it uses an in-memory H2 database and loads sample
inspections and damage reports of the sample building `7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d`.

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Without a profile it connects to PostgreSQL. Create the database and its tables with
[`docs/schema.sql`](docs/schema.sql) first; Hibernate only validates them.

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8088` |
| `MAINTENANCE_DB_URL` | `jdbc:postgresql://localhost:5432/maintenancedb` |
| `MAINTENANCE_DB_USERNAME` | `postgres` |
| `MAINTENANCE_DB_PASSWORD` | `postgres` |

## Test

```sh
./mvnw test
```

The unit tests use H2. The Cucumber scenarios in
[`src/test/resources/com/rumi/maintenance/maintenance.feature`](src/test/resources/com/rumi/maintenance/maintenance.feature)
are tagged with their user story (`@US17`, `@US18`, `@US21`, `@US22`) and run against PostgreSQL 16 in a
container (Testcontainers), so Docker must be running.

## Structure

```
com.rumi.maintenance
├── domain
│   ├── model          Inspection, InspectionFinding, DamageReport, PhotoEvidence, statuses and severity
│   └── repository     InspectionRepository, DamageReportRepository
├── application        InspectionApplicationService, DamageReportApplicationService, DamageBoard, commands
└── infrastructure
    ├── persistence    JPA entities and repositories (maintenancedb)
    ├── web            InspectionController, DamageReportController, GatewayUser, error handling
    ├── seed           sample data of the dev profile
    └── config         clock (time zone America/Lima for calendar dates)
```
