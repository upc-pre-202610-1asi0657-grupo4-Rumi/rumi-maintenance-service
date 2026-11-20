@US17 @US18 @US21 @US22
Feature: Building maintenance backed by PostgreSQL
  As the administrator of a building
  I want to schedule inspections, record their findings (US17 Programar inspecciones, US18 Registrar hallazgos)
  and review the damage that residents report (US21 Reportar daño comunitario, US22 Tablero de reportes de daños)
  So that the building keeps a documented and prioritized maintenance plan

  Background:
    Given a building of the Rumi network

  @US17
  Scenario: A scheduled inspection is registered in the calendar of the building as pending
    When the administrator schedules an inspection in 7 days with engineer "Ing. Carla Mendoza"
    Then the response status is 201
    And the calendar of the building has 1 inspection with status "PENDING" and engineer "Ing. Carla Mendoza"

  @US17
  Scenario: An inspection cannot be scheduled for a past date
    When the administrator schedules an inspection in -1 days with engineer "Ing. Carla Mendoza"
    Then the response status is 400
    And the problem detail says "Scheduled date cannot be in the past"
    And the calendar of the building has no inspections

  @US18
  Scenario: A finding is recorded with its date, severity and photo while the inspection is in progress
    Given the administrator schedules an inspection in 0 days with engineer "Ing. Carla Mendoza"
    And the engineer starts the inspection
    When the administrator records a "HIGH" finding "Diagonal crack in beam B-2" with photo "https://storage.example.com/rumi/b2.jpg"
    Then the response status is 201
    And the inspection has 1 finding with severity "HIGH", photo "https://storage.example.com/rumi/b2.jpg" and a recording date
    When the administrator completes the inspection
    Then the response status is 200
    And the inspection status is "COMPLETED"

  @US18
  Scenario: Findings cannot be recorded before the visit starts
    Given the administrator schedules an inspection in 0 days with engineer "Ing. Carla Mendoza"
    When the administrator records a "LOW" finding "Peeling paint" with photo "https://storage.example.com/rumi/paint.jpg"
    Then the response status is 409

  @US21
  Scenario: A resident reports a damage with photo and zone and the administrator sees it
    When a resident of the building reports "Crack next to the elevator" in zone "FLOOR-2-EAST" with photo "https://storage.example.com/rumi/wall.jpg"
    Then the response status is 201
    And the damage board of the building shows 1 "UNASSESSED" report in zone "FLOOR-2-EAST"

  @US21
  Scenario: A resident cannot report damage in a building that is not theirs
    When a resident of another building reports "Crack next to the elevator" in zone "FLOOR-2-EAST" with photo "https://storage.example.com/rumi/wall.jpg"
    Then the response status is 403

  @US22
  Scenario: The damage board groups the reports by zone and severity, most severe zone first
    Given a resident of the building reports "Loose handrail" in zone "STAIRWELL-B" with photo "https://storage.example.com/rumi/rail.jpg"
    And the administrator assigns severity "MEDIUM" to the last report
    And a resident of the building reports "Spalled column" in zone "FLOOR-2-EAST" with photo "https://storage.example.com/rumi/column.jpg"
    And the administrator assigns severity "HIGH" to the last report
    And a resident of the building reports "Crack next to the elevator" in zone "FLOOR-2-EAST" with photo "https://storage.example.com/rumi/wall.jpg"
    Then the damage board lists the zones "FLOOR-2-EAST, STAIRWELL-B" in that order
    And zone "FLOOR-2-EAST" groups its reports as "HIGH, UNASSESSED"

  @US17 @US22
  Scenario: An inspection scheduled for a damage report puts the report under review until it is resolved
    Given a resident of the building reports "Spalled column" in zone "FLOOR-2-EAST" with photo "https://storage.example.com/rumi/column.jpg"
    When the administrator schedules an inspection in 3 days for the last report
    Then the last report is "UNDER_REVIEW" and linked to the inspection
    When the administrator resolves the last report with severity "LOW"
    Then the damage board of the building has no reports
