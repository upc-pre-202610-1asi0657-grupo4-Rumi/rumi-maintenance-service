# rumi-maintenance-service

Building Maintenance service of **Rumi**, the structural monitoring platform by Kuntur Labs.

> **SKELETON: functionality planned for later sprints. The health endpoint is NOT counted as an implemented functional endpoint.**

| | |
|---|---|
| Bounded context | Building Maintenance |
| Port | `8088` |
| Gateway routes | `/api/v1/inspections/**`, `/api/v1/damage-reports/**` |
| Base package | `com.rumi.maintenance` |

## Purpose

Will own inspection scheduling (US17), inspection findings (US18), damage reports (US21) and the damage board (US22).
None of it is implemented yet: this repository only fixes the service boundary, its port
and its place behind the API gateway.

## Origin

New service. It has no code in the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend); it is one of the
bounded contexts of the target architecture defined when the monolith was decomposed.

## Endpoints

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| GET | `/api/v1/inspections/health` | Check that the service is running | none | `200` `HealthResponse` | none | skeleton |

Implemented functional endpoints: 0. Skeleton endpoints: 1.

```json
{
  "status": "UP",
  "service": "rumi-maintenance-service"
}
```

## API documentation

- Swagger UI: <http://localhost:8088/swagger-ui.html>
- OpenAPI spec: <http://localhost:8088/v3/api-docs>
- Exported spec: [`docs/openapi.json`](docs/openapi.json)

## Run

Requirements: JDK 21, Maven.

```sh
mvn spring-boot:run
```

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8088` |

## Test

```sh
mvn test
```

## Structure

```
com.rumi.maintenance
├── domain             empty
├── application        empty
├── infrastructure     OpenAPI configuration
└── interfaces.rest    health endpoint
```
