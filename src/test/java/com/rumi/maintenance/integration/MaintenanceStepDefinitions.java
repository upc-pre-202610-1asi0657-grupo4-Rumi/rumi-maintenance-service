package com.rumi.maintenance.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Everything the administrator and the residents do goes through the REST API, with the headers that the
 * API gateway adds after validating the token.
 */
public class MaintenanceStepDefinitions {

    private static final ZoneId BUILDINGS_TIME_ZONE = ZoneId.of("America/Lima");

    private final MaintenanceIntegrationTestConfiguration context;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID buildingId;
    private String inspectionId;
    private String lastReportId;
    private ResponseEntity<String> response;

    public MaintenanceStepDefinitions(MaintenanceIntegrationTestConfiguration context) {
        this.context = context;
    }

    @Before
    public void resetScenarioState() {
        buildingId = null;
        inspectionId = null;
        lastReportId = null;
        response = null;
    }

    @Given("a building of the Rumi network")
    public void aBuilding() {
        buildingId = UUID.randomUUID();
    }

    @When("the administrator schedules an inspection in {int} days with engineer {string}")
    public void scheduleInspection(int days, String engineer) throws Exception {
        scheduleInspection(days, engineer, null);
    }

    @When("the administrator schedules an inspection in {int} days for the last report")
    public void scheduleInspectionForTheLastReport(int days) throws Exception {
        scheduleInspection(days, "Ing. Carla Mendoza", lastReportId);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Given("the engineer starts the inspection")
    public void startInspection() {
        response = send(HttpMethod.PATCH, "/api/v1/inspections/" + inspectionId + "/start", null, administrator());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @When("the administrator records a {string} finding {string} with photo {string}")
    public void recordFinding(String severity, String notes, String photoUrl) {
        String body = "{\"notes\":\"" + notes + "\",\"severity\":\"" + severity + "\",\"photoUrl\":\""
                + photoUrl + "\"}";
        response = send(HttpMethod.POST, "/api/v1/inspections/" + inspectionId + "/findings", body,
                administrator());
    }

    @When("the administrator completes the inspection")
    public void completeInspection() {
        response = send(HttpMethod.PATCH, "/api/v1/inspections/" + inspectionId + "/complete", null,
                administrator());
    }

    @When("a resident of the building reports {string} in zone {string} with photo {string}")
    public void residentReports(String description, String zone, String photoUrl) throws Exception {
        reportDamage(description, zone, photoUrl, buildingId);
        if (response.getStatusCode().value() == 201) {
            lastReportId = json().get("id").asText();
        }
    }

    @When("a resident of another building reports {string} in zone {string} with photo {string}")
    public void residentOfAnotherBuildingReports(String description, String zone, String photoUrl) {
        reportDamage(description, zone, photoUrl, UUID.randomUUID());
    }

    @Given("the administrator assigns severity {string} to the last report")
    public void assignSeverity(String severity) {
        review(severity, "UNDER_REVIEW");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @When("the administrator resolves the last report with severity {string}")
    public void resolveLastReport(String severity) {
        review(severity, "RESOLVED");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the problem detail says {string}")
    public void problemDetailSays(String detail) throws Exception {
        assertThat(json().get("detail").asText()).isEqualTo(detail);
    }

    @Then("the calendar of the building has {int} inspection with status {string} and engineer {string}")
    public void calendarHasInspection(int count, String status, String engineer) throws Exception {
        JsonNode inspections = listInspections();
        assertThat(inspections).hasSize(count);
        assertThat(inspections.get(0).get("status").asText()).isEqualTo(status);
        assertThat(inspections.get(0).get("engineerName").asText()).isEqualTo(engineer);
        assertThat(inspections.get(0).get("buildingId").asText()).isEqualTo(buildingId.toString());
    }

    @Then("the calendar of the building has no inspections")
    public void calendarIsEmpty() throws Exception {
        assertThat(listInspections()).isEmpty();
    }

    @Then("the inspection has {int} finding with severity {string}, photo {string} and a recording date")
    public void inspectionHasFinding(int count, String severity, String photoUrl) throws Exception {
        JsonNode findings = json().get("findings");
        assertThat(findings).hasSize(count);
        assertThat(findings.get(0).get("severity").asText()).isEqualTo(severity);
        assertThat(findings.get(0).get("photoUrl").asText()).isEqualTo(photoUrl);
        assertThat(findings.get(0).get("recordedAt").isNull()).isFalse();
        assertThat(json().get("buildingId").asText()).isEqualTo(buildingId.toString());
    }

    @Then("the inspection status is {string}")
    public void inspectionStatusIs(String status) throws Exception {
        assertThat(json().get("status").asText()).isEqualTo(status);
    }

    @Then("the damage board of the building shows {int} {string} report in zone {string}")
    public void boardShowsReport(int count, String severity, String zone) throws Exception {
        JsonNode board = board();
        assertThat(board.get("totalReports").asInt()).isEqualTo(count);
        JsonNode zoneNode = board.get("zones").get(0);
        assertThat(zoneNode.get("zone").asText()).isEqualTo(zone);
        assertThat(zoneNode.get("groups").get(0).get("severity").asText()).isEqualTo(severity);
        assertThat(zoneNode.get("groups").get(0).get("reports").get(0).get("id").asText()).isEqualTo(lastReportId);
    }

    @Then("the damage board lists the zones {string} in that order")
    public void boardListsZones(String zones) throws Exception {
        List<String> actual = new ArrayList<>();
        board().get("zones").forEach(zone -> actual.add(zone.get("zone").asText()));
        assertThat(actual).isEqualTo(Arrays.asList(zones.split(",\\s*")));
    }

    @Then("zone {string} groups its reports as {string}")
    public void zoneGroups(String zone, String severities) throws Exception {
        List<String> actual = new ArrayList<>();
        board().get("zones").forEach(zoneNode -> {
            if (zoneNode.get("zone").asText().equals(zone)) {
                zoneNode.get("groups").forEach(group -> actual.add(group.get("severity").asText()));
            }
        });
        assertThat(actual).isEqualTo(Arrays.asList(severities.split(",\\s*")));
    }

    @Then("the last report is {string} and linked to the inspection")
    public void lastReportIsLinked(String status) throws Exception {
        JsonNode report = findLastReportOnTheBoard();
        assertThat(report.get("status").asText()).isEqualTo(status);
        assertThat(report.get("generatedInspectionId").asText()).isEqualTo(inspectionId);
    }

    @Then("the damage board of the building has no reports")
    public void boardIsEmpty() throws Exception {
        JsonNode board = board();
        assertThat(board.get("totalReports").asInt()).isZero();
        assertThat(board.get("zones")).isEmpty();
    }

    private void scheduleInspection(int days, String engineer, String damageReportId) throws Exception {
        LocalDate date = LocalDate.now(BUILDINGS_TIME_ZONE).plusDays(days);
        String body = "{\"buildingId\":\"" + buildingId + "\",\"engineerName\":\"" + engineer
                + "\",\"scheduledDate\":\"" + date + "\""
                + (damageReportId == null ? "" : ",\"damageReportId\":\"" + damageReportId + "\"") + "}";
        response = send(HttpMethod.POST, "/api/v1/inspections", body, administrator());
        if (response.getStatusCode().value() == 201) {
            inspectionId = json().get("id").asText();
        }
    }

    private void reportDamage(String description, String zone, String photoUrl, UUID buildingOfTheResident) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Role", "RESIDENT");
        headers.set("X-Building-Id", buildingOfTheResident.toString());
        headers.set("X-User-Id", UUID.randomUUID().toString());
        String body = "{\"buildingId\":\"" + buildingId + "\",\"description\":\"" + description + "\",\"zone\":\""
                + zone + "\",\"photoUrl\":\"" + photoUrl + "\"}";
        response = send(HttpMethod.POST, "/api/v1/damage-reports", body, headers);
    }

    private void review(String severity, String status) {
        response = send(HttpMethod.PATCH, "/api/v1/damage-reports/" + lastReportId + "/review",
                "{\"severity\":\"" + severity + "\",\"status\":\"" + status + "\"}", administrator());
    }

    private JsonNode findLastReportOnTheBoard() throws Exception {
        for (JsonNode zone : board().get("zones")) {
            for (JsonNode group : zone.get("groups")) {
                for (JsonNode report : group.get("reports")) {
                    if (report.get("id").asText().equals(lastReportId)) {
                        return report;
                    }
                }
            }
        }
        throw new AssertionError("Report " + lastReportId + " is not on the damage board");
    }

    private JsonNode listInspections() throws Exception {
        ResponseEntity<String> list = send(HttpMethod.GET, "/api/v1/inspections?buildingId=" + buildingId, null,
                administrator());
        assertThat(list.getStatusCode().value()).isEqualTo(200);
        return objectMapper.readTree(list.getBody());
    }

    private JsonNode board() throws Exception {
        ResponseEntity<String> board = send(HttpMethod.GET, "/api/v1/damage-reports/board?buildingId=" + buildingId,
                null, administrator());
        assertThat(board.getStatusCode().value()).isEqualTo(200);
        return objectMapper.readTree(board.getBody());
    }

    private JsonNode json() throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private static HttpHeaders administrator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Role", "ADMINISTRATOR");
        return headers;
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body, HttpHeaders headers) {
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return context.http.exchange(context.url(path), method, new HttpEntity<>(body, headers), String.class);
    }
}
