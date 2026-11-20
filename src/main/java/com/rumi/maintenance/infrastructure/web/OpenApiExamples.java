package com.rumi.maintenance.infrastructure.web;

/**
 * Example payloads shown in the OpenAPI documentation. The identifiers are those of the sample data that
 * the dev profile loads.
 */
final class OpenApiExamples {

    static final String PROBLEM_JSON = "application/problem+json";

    static final String SCHEDULE_INSPECTION_REQUEST = """
            {
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "engineerName": "Ing. Carla Mendoza",
              "scheduledDate": "2026-11-27",
              "damageReportId": "8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13"
            }""";

    static final String PENDING_INSPECTION_RESPONSE = """
            {
              "id": "3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21",
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "engineerName": "Ing. Carla Mendoza",
              "scheduledDate": "2026-11-27",
              "status": "PENDING",
              "highestSeverity": null,
              "createdAt": "2026-11-20T16:00:00Z",
              "startedAt": null,
              "completedAt": null,
              "findings": []
            }""";

    static final String IN_PROGRESS_INSPECTION_RESPONSE = """
            {
              "id": "3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21",
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "engineerName": "Ing. Carla Mendoza",
              "scheduledDate": "2026-11-27",
              "status": "IN_PROGRESS",
              "highestSeverity": null,
              "createdAt": "2026-11-20T16:00:00Z",
              "startedAt": "2026-11-27T15:00:00Z",
              "completedAt": null,
              "findings": []
            }""";

    static final String INSPECTION_WITH_FINDING_RESPONSE = """
            {
              "id": "3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21",
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "engineerName": "Ing. Carla Mendoza",
              "scheduledDate": "2026-11-27",
              "status": "IN_PROGRESS",
              "highestSeverity": "HIGH",
              "createdAt": "2026-11-20T16:00:00Z",
              "startedAt": "2026-11-27T15:00:00Z",
              "completedAt": null,
              "findings": [
                {
                  "id": "5b7d9f1a-3c5e-4a7b-9d1f-3e5a7c9e1b24",
                  "notes": "Diagonal crack of 3 mm in the beam of axis B-2",
                  "severity": "HIGH",
                  "photoUrl": "https://storage.example.com/rumi/inspections/beam-b2.jpg",
                  "photoTakenAt": "2026-11-27T15:10:00Z",
                  "recordedAt": "2026-11-27T15:12:00Z"
                }
              ]
            }""";

    static final String COMPLETED_INSPECTION_RESPONSE = """
            {
              "id": "3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21",
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "engineerName": "Ing. Carla Mendoza",
              "scheduledDate": "2026-11-27",
              "status": "COMPLETED",
              "highestSeverity": "HIGH",
              "createdAt": "2026-11-20T16:00:00Z",
              "startedAt": "2026-11-27T15:00:00Z",
              "completedAt": "2026-11-27T16:30:00Z",
              "findings": [
                {
                  "id": "5b7d9f1a-3c5e-4a7b-9d1f-3e5a7c9e1b24",
                  "notes": "Diagonal crack of 3 mm in the beam of axis B-2",
                  "severity": "HIGH",
                  "photoUrl": "https://storage.example.com/rumi/inspections/beam-b2.jpg",
                  "photoTakenAt": "2026-11-27T15:10:00Z",
                  "recordedAt": "2026-11-27T15:12:00Z"
                }
              ]
            }""";

    static final String INSPECTION_LIST_RESPONSE = "[" + PENDING_INSPECTION_RESPONSE + "\n]";

    static final String FINDING_REQUEST = """
            {
              "notes": "Diagonal crack of 3 mm in the beam of axis B-2",
              "severity": "HIGH",
              "photoUrl": "https://storage.example.com/rumi/inspections/beam-b2.jpg",
              "photoTakenAt": "2026-11-27T15:10:00Z"
            }""";

    static final String DAMAGE_REPORT_REQUEST = """
            {
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "description": "Crack in the wall next to the elevator",
              "zone": "FLOOR-2-EAST",
              "photoUrl": "https://storage.example.com/rumi/damage/floor2-wall.jpg",
              "photoTakenAt": "2026-11-20T13:45:00Z"
            }""";

    static final String DAMAGE_REPORT_RESPONSE = """
            {
              "id": "8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13",
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "residentUserId": "d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70",
              "description": "Crack in the wall next to the elevator",
              "zone": "FLOOR-2-EAST",
              "photoUrl": "https://storage.example.com/rumi/damage/floor2-wall.jpg",
              "photoTakenAt": "2026-11-20T13:45:00Z",
              "severity": null,
              "status": "REPORTED",
              "generatedInspectionId": null,
              "reportedAt": "2026-11-20T13:50:00Z",
              "reviewedAt": null,
              "resolvedAt": null
            }""";

    static final String DAMAGE_REVIEW_REQUEST = """
            {
              "severity": "HIGH",
              "status": "UNDER_REVIEW"
            }""";

    static final String REVIEWED_DAMAGE_REPORT_RESPONSE = """
            {
              "id": "8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13",
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "residentUserId": "d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70",
              "description": "Crack in the wall next to the elevator",
              "zone": "FLOOR-2-EAST",
              "photoUrl": "https://storage.example.com/rumi/damage/floor2-wall.jpg",
              "photoTakenAt": "2026-11-20T13:45:00Z",
              "severity": "HIGH",
              "status": "UNDER_REVIEW",
              "generatedInspectionId": null,
              "reportedAt": "2026-11-20T13:50:00Z",
              "reviewedAt": "2026-11-20T16:05:00Z",
              "resolvedAt": null
            }""";

    static final String DAMAGE_BOARD_RESPONSE = """
            {
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "totalReports": 3,
              "zones": [
                {
                  "zone": "FLOOR-2-EAST",
                  "totalReports": 2,
                  "highestSeverity": "HIGH",
                  "groups": [
                    {
                      "severity": "HIGH",
                      "reports": [
                        {
                          "id": "9d3f5b7a-2c4e-4a6b-8d0f-3e5a7b9d1f35",
                          "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
                          "residentUserId": "d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70",
                          "description": "Column with spalled concrete in the parking access",
                          "zone": "FLOOR-2-EAST",
                          "photoUrl": "https://storage.example.com/rumi/damage/floor2-column.jpg",
                          "photoTakenAt": null,
                          "severity": "HIGH",
                          "status": "UNDER_REVIEW",
                          "generatedInspectionId": "6a8c0e2b-4d6f-4a1c-8e3b-5d7f9b1d3f46",
                          "reportedAt": "2026-11-10T14:00:00Z",
                          "reviewedAt": "2026-11-10T16:00:00Z",
                          "resolvedAt": null
                        }
                      ]
                    },
                    {
                      "severity": "UNASSESSED",
                      "reports": [
                        {
                          "id": "8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13",
                          "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
                          "residentUserId": "d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70",
                          "description": "Crack in the wall next to the elevator",
                          "zone": "FLOOR-2-EAST",
                          "photoUrl": "https://storage.example.com/rumi/damage/floor2-wall.jpg",
                          "photoTakenAt": "2026-11-20T13:45:00Z",
                          "severity": null,
                          "status": "REPORTED",
                          "generatedInspectionId": null,
                          "reportedAt": "2026-11-20T13:50:00Z",
                          "reviewedAt": null,
                          "resolvedAt": null
                        }
                      ]
                    }
                  ]
                },
                {
                  "zone": "STAIRWELL-B",
                  "totalReports": 1,
                  "highestSeverity": "MEDIUM",
                  "groups": [
                    {
                      "severity": "MEDIUM",
                      "reports": [
                        {
                          "id": "1e4a6c8b-3d5f-4b7c-9e1a-4f6b8d0f2a57",
                          "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
                          "residentUserId": "d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70",
                          "description": "Loose handrail between the third and fourth floor",
                          "zone": "STAIRWELL-B",
                          "photoUrl": "https://storage.example.com/rumi/damage/stairwell-handrail.jpg",
                          "photoTakenAt": null,
                          "severity": "MEDIUM",
                          "status": "UNDER_REVIEW",
                          "generatedInspectionId": null,
                          "reportedAt": "2026-11-15T09:30:00Z",
                          "reviewedAt": "2026-11-15T12:00:00Z",
                          "resolvedAt": null
                        }
                      ]
                    }
                  ]
                }
              ]
            }""";

    static final String VALIDATION_ERROR = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "Invalid request content.",
              "instance": "/api/v1/inspections",
              "errors": {
                "engineerName": "must not be blank"
              }
            }""";

    static final String PAST_DATE_ERROR = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "Scheduled date cannot be in the past",
              "instance": "/api/v1/inspections"
            }""";

    static final String PHOTO_URL_ERROR = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "Photo URL must be an http or https address",
              "instance": "/api/v1/damage-reports"
            }""";

    static final String MISSING_BUILDING_ID_ERROR = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "Required parameter 'buildingId' is not present.",
              "instance": "/api/v1/inspections"
            }""";

    static final String RESIDENT_NOT_ADMINISTRATOR_ERROR = """
            {
              "type": "about:blank",
              "title": "Forbidden",
              "status": 403,
              "detail": "Only the building administrator can manage inspections",
              "instance": "/api/v1/inspections"
            }""";

    static final String RESIDENT_OTHER_BUILDING_ERROR = """
            {
              "type": "about:blank",
              "title": "Forbidden",
              "status": 403,
              "detail": "You cannot report damage in building 1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9: it is not one of your buildings",
              "instance": "/api/v1/damage-reports"
            }""";

    static final String INSPECTION_NOT_FOUND_ERROR = """
            {
              "type": "about:blank",
              "title": "Not Found",
              "status": 404,
              "detail": "Inspection 3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21 was not found",
              "instance": "/api/v1/inspections/3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21/start"
            }""";

    static final String DAMAGE_REPORT_NOT_FOUND_ERROR = """
            {
              "type": "about:blank",
              "title": "Not Found",
              "status": 404,
              "detail": "Damage report 8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13 was not found",
              "instance": "/api/v1/damage-reports/8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13/review"
            }""";

    static final String INSPECTION_NOT_PENDING_ERROR = """
            {
              "type": "about:blank",
              "title": "Conflict",
              "status": 409,
              "detail": "Inspection 3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21 is IN_PROGRESS and cannot be started",
              "instance": "/api/v1/inspections/3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21/start"
            }""";

    static final String INSPECTION_NOT_IN_PROGRESS_ERROR = """
            {
              "type": "about:blank",
              "title": "Conflict",
              "status": 409,
              "detail": "Inspection 3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21 is PENDING and cannot receive findings",
              "instance": "/api/v1/inspections/3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21/findings"
            }""";

    static final String INSPECTION_WITHOUT_FINDINGS_ERROR = """
            {
              "type": "about:blank",
              "title": "Conflict",
              "status": 409,
              "detail": "Inspection 3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21 has no findings yet; record at least one before completing it",
              "instance": "/api/v1/inspections/3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21/complete"
            }""";

    static final String DAMAGE_REPORT_RESOLVED_ERROR = """
            {
              "type": "about:blank",
              "title": "Conflict",
              "status": 409,
              "detail": "Damage report 8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13 is already RESOLVED and cannot be reviewed",
              "instance": "/api/v1/damage-reports/8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13/review"
            }""";

    private OpenApiExamples() {
    }
}
