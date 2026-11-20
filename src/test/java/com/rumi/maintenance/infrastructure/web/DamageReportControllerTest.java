package com.rumi.maintenance.infrastructure.web;

import com.rumi.maintenance.MaintenanceFixtures;
import com.rumi.maintenance.application.DamageBoard;
import com.rumi.maintenance.application.DamageReportApplicationService;
import com.rumi.maintenance.application.DamageReportNotFoundException;
import com.rumi.maintenance.application.ReportDamageCommand;
import com.rumi.maintenance.application.ReviewDamageReportCommand;
import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.FindingSeverity;
import com.rumi.maintenance.domain.model.MaintenanceStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DamageReportController.class)
@Import(DamageReportDtoMapper.class)
class DamageReportControllerTest {

    private static final UUID BUILDING_ID = MaintenanceFixtures.BUILDING_ID;
    private static final UUID OTHER_BUILDING_ID = MaintenanceFixtures.OTHER_BUILDING_ID;
    private static final UUID RESIDENT_ID = MaintenanceFixtures.RESIDENT_ID;
    private static final String REPORT_REQUEST = """
            {
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "description": "Crack in the wall next to the elevator",
              "zone": "FLOOR-2-EAST",
              "photoUrl": "https://storage.example.com/rumi/damage/floor2-wall.jpg"
            }""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DamageReportApplicationService damageReportService;

    private ReportDamageCommand command(UUID residentId) {
        return new ReportDamageCommand(BUILDING_ID, residentId, "Crack in the wall next to the elevator",
                "FLOOR-2-EAST", MaintenanceFixtures.PHOTO_URL, null);
    }

    @Test
    void aResidentReportsDamageInTheirBuilding() throws Exception {
        DamageReport report = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");
        when(damageReportService.reportDamage(command(RESIDENT_ID))).thenReturn(report);

        mockMvc.perform(post("/api/v1/damage-reports")
                        .header("X-Role", "RESIDENT")
                        .header("X-Building-Id", BUILDING_ID.toString())
                        .header("X-User-Id", RESIDENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REPORT_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(report.getId().toString()))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.zone").value("FLOOR-2-EAST"))
                .andExpect(jsonPath("$.residentUserId").value(RESIDENT_ID.toString()))
                .andExpect(jsonPath("$.severity").doesNotExist())
                .andExpect(jsonPath("$.reportedAt").value("2026-11-20T16:00:00Z"));
    }

    @Test
    void aResidentCannotReportDamageInAnotherBuilding() throws Exception {
        mockMvc.perform(post("/api/v1/damage-reports")
                        .header("X-Role", "RESIDENT")
                        .header("X-Building-Id", OTHER_BUILDING_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REPORT_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("You cannot report damage in building " + BUILDING_ID
                        + ": it is not one of your buildings"));

        verifyNoInteractions(damageReportService);
    }

    @Test
    void rejectsAReportWithoutPhotoOrZone() throws Exception {
        mockMvc.perform(post("/api/v1/damage-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingId\":\"" + BUILDING_ID + "\",\"description\":\"Crack\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.zone").exists())
                .andExpect(jsonPath("$.errors.photoUrl").exists());

        verifyNoInteractions(damageReportService);
    }

    @Test
    void answersBadRequestForAPhotoThatIsNotAWebAddress() throws Exception {
        when(damageReportService.reportDamage(command(null)))
                .thenThrow(new IllegalArgumentException("Photo URL must be an http or https address"));

        mockMvc.perform(post("/api/v1/damage-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REPORT_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Photo URL must be an http or https address"));
    }

    @Test
    void returnsTheBoardGroupedByZoneAndSeverity() throws Exception {
        DamageReport high = MaintenanceFixtures.reviewedDamage("FLOOR-2-EAST", FindingSeverity.HIGH,
                MaintenanceFixtures.NOW.minusSeconds(600));
        DamageReport unassessed = MaintenanceFixtures.reportedDamage("FLOOR-2-EAST");
        DamageBoard board = new DamageBoard(BUILDING_ID, 2, List.of(new DamageBoard.Zone(
                "FLOOR-2-EAST", 2, FindingSeverity.HIGH, List.of(
                        new DamageBoard.SeverityGroup(FindingSeverity.HIGH, List.of(high)),
                        new DamageBoard.SeverityGroup(null, List.of(unassessed))))));
        when(damageReportService.getBoard(BUILDING_ID, false)).thenReturn(board);

        mockMvc.perform(get("/api/v1/damage-reports/board").param("buildingId", BUILDING_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(2))
                .andExpect(jsonPath("$.zones[0].zone").value("FLOOR-2-EAST"))
                .andExpect(jsonPath("$.zones[0].highestSeverity").value("HIGH"))
                .andExpect(jsonPath("$.zones[0].groups[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.zones[0].groups[0].reports[0].id").value(high.getId().toString()))
                .andExpect(jsonPath("$.zones[0].groups[1].severity").value("UNASSESSED"))
                .andExpect(jsonPath("$.zones[0].groups[1].reports[0].id").value(unassessed.getId().toString()));
    }

    @Test
    void theBoardCanIncludeResolvedReports() throws Exception {
        when(damageReportService.getBoard(BUILDING_ID, true)).thenReturn(new DamageBoard(BUILDING_ID, 0, List.of()));

        mockMvc.perform(get("/api/v1/damage-reports/board")
                        .param("buildingId", BUILDING_ID.toString())
                        .param("includeResolved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zones").isEmpty());
    }

    @Test
    void aResidentCannotSeeTheBoardNorReviewReports() throws Exception {
        mockMvc.perform(get("/api/v1/damage-reports/board")
                        .param("buildingId", BUILDING_ID.toString())
                        .header("X-Role", "RESIDENT")
                        .header("X-Building-Id", BUILDING_ID.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only the building administrator can manage the damage board"));
        mockMvc.perform(patch("/api/v1/damage-reports/{reportId}/review", UUID.randomUUID())
                        .header("X-Role", "RESIDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"HIGH\",\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(damageReportService);
    }

    @Test
    void theAdministratorReviewsAReport() throws Exception {
        DamageReport report = MaintenanceFixtures.reviewedDamage("FLOOR-2-EAST", FindingSeverity.HIGH,
                MaintenanceFixtures.NOW);
        when(damageReportService.reviewDamageReport(new ReviewDamageReportCommand(
                report.getId(), FindingSeverity.HIGH, DamageReportStatus.UNDER_REVIEW))).thenReturn(report);

        mockMvc.perform(patch("/api/v1/damage-reports/{reportId}/review", report.getId())
                        .header("X-Role", "ADMINISTRATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"HIGH\",\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.reviewedAt").value("2026-11-20T16:01:00Z"));
    }

    @Test
    void rejectsAReviewWithoutSeverity() throws Exception {
        mockMvc.perform(patch("/api/v1/damage-reports/{reportId}/review", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.severity").exists());

        verifyNoInteractions(damageReportService);
    }

    @Test
    void answersNotFoundAndConflictForTheReview() throws Exception {
        UUID unknown = UUID.randomUUID();
        UUID resolved = UUID.randomUUID();
        when(damageReportService.reviewDamageReport(new ReviewDamageReportCommand(
                unknown, FindingSeverity.LOW, DamageReportStatus.RESOLVED)))
                .thenThrow(new DamageReportNotFoundException(unknown));
        when(damageReportService.reviewDamageReport(new ReviewDamageReportCommand(
                resolved, FindingSeverity.LOW, DamageReportStatus.RESOLVED)))
                .thenThrow(new MaintenanceStateException(
                        "Damage report " + resolved + " is already RESOLVED and cannot be resolved"));

        mockMvc.perform(patch("/api/v1/damage-reports/{reportId}/review", unknown)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"LOW\",\"status\":\"RESOLVED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Damage report " + unknown + " was not found"));
        mockMvc.perform(patch("/api/v1/damage-reports/{reportId}/review", resolved)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"LOW\",\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict());
    }
}
