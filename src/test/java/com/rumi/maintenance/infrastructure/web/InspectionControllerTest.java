package com.rumi.maintenance.infrastructure.web;

import com.rumi.maintenance.MaintenanceFixtures;
import com.rumi.maintenance.application.InspectionApplicationService;
import com.rumi.maintenance.application.InspectionNotFoundException;
import com.rumi.maintenance.application.RecordFindingCommand;
import com.rumi.maintenance.application.ScheduleInspectionCommand;
import com.rumi.maintenance.domain.model.FindingSeverity;
import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionStatus;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InspectionController.class)
@Import(InspectionDtoMapper.class)
class InspectionControllerTest {

    private static final UUID BUILDING_ID = MaintenanceFixtures.BUILDING_ID;
    private static final String SCHEDULE_REQUEST = """
            {
              "buildingId": "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d",
              "engineerName": "Ing. Carla Mendoza",
              "scheduledDate": "2026-11-27"
            }""";
    private static final String FINDING_REQUEST = """
            {
              "notes": "Diagonal crack of 3 mm in the beam of axis B-2",
              "severity": "HIGH",
              "photoUrl": "https://storage.example.com/rumi/damage/floor2-wall.jpg"
            }""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InspectionApplicationService inspectionService;

    @Test
    void schedulesAPendingInspection() throws Exception {
        Inspection inspection = MaintenanceFixtures.pendingInspection();
        when(inspectionService.scheduleInspection(new ScheduleInspectionCommand(
                BUILDING_ID, "Ing. Carla Mendoza", MaintenanceFixtures.NEXT_WEEK, null))).thenReturn(inspection);

        mockMvc.perform(post("/api/v1/inspections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SCHEDULE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(inspection.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.engineerName").value("Ing. Carla Mendoza"))
                .andExpect(jsonPath("$.scheduledDate").value("2026-11-27"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-20T16:00:00Z"))
                .andExpect(jsonPath("$.findings").isEmpty());
    }

    @Test
    void rejectsAnInspectionWithoutEngineer() throws Exception {
        mockMvc.perform(post("/api/v1/inspections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingId\":\"" + BUILDING_ID + "\",\"scheduledDate\":\"2026-11-27\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.engineerName").exists());

        verifyNoInteractions(inspectionService);
    }

    @Test
    void answersBadRequestForAPastDate() throws Exception {
        when(inspectionService.scheduleInspection(new ScheduleInspectionCommand(
                BUILDING_ID, "Ing. Carla Mendoza", MaintenanceFixtures.NEXT_WEEK, null)))
                .thenThrow(new IllegalArgumentException("Scheduled date cannot be in the past"));

        mockMvc.perform(post("/api/v1/inspections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SCHEDULE_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Scheduled date cannot be in the past"));
    }

    @Test
    void aResidentCannotScheduleOrListInspections() throws Exception {
        mockMvc.perform(post("/api/v1/inspections")
                        .header("X-Role", "RESIDENT")
                        .header("X-Building-Id", BUILDING_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SCHEDULE_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Only the building administrator can manage inspections"));
        mockMvc.perform(get("/api/v1/inspections")
                        .param("buildingId", BUILDING_ID.toString())
                        .header("X-Role", "RESIDENT"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(inspectionService);
    }

    @Test
    void listsTheCalendarOfABuildingWithTheFindings() throws Exception {
        Inspection inspection = MaintenanceFixtures.inspectionWithFinding(FindingSeverity.HIGH);
        when(inspectionService.listInspections(BUILDING_ID, InspectionStatus.IN_PROGRESS))
                .thenReturn(List.of(inspection));

        mockMvc.perform(get("/api/v1/inspections")
                        .param("buildingId", BUILDING_ID.toString())
                        .param("status", "IN_PROGRESS")
                        .header("X-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].highestSeverity").value("HIGH"))
                .andExpect(jsonPath("$[0].findings[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].findings[0].photoUrl").value(MaintenanceFixtures.PHOTO_URL));
    }

    @Test
    void rejectsAListWithoutBuildingOrWithAnUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/v1/inspections"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/v1/inspections")
                        .param("buildingId", BUILDING_ID.toString())
                        .param("status", "DONE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inspectionService);
    }

    @Test
    void startsAnInspection() throws Exception {
        Inspection inspection = MaintenanceFixtures.inProgressInspection();
        when(inspectionService.startInspection(inspection.getId())).thenReturn(inspection);

        mockMvc.perform(patch("/api/v1/inspections/{inspectionId}/start", inspection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").value("2026-11-20T16:01:00Z"));
    }

    @Test
    void answersConflictWhenTheInspectionCannotStart() throws Exception {
        UUID inspectionId = UUID.randomUUID();
        when(inspectionService.startInspection(inspectionId)).thenThrow(new MaintenanceStateException(
                "Inspection " + inspectionId + " is IN_PROGRESS and cannot be started"));

        mockMvc.perform(patch("/api/v1/inspections/{inspectionId}/start", inspectionId))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("Inspection " + inspectionId + " is IN_PROGRESS and cannot be started"));
    }

    @Test
    void answersNotFoundForAnUnknownInspection() throws Exception {
        UUID inspectionId = UUID.randomUUID();
        when(inspectionService.completeInspection(inspectionId))
                .thenThrow(new InspectionNotFoundException(inspectionId));

        mockMvc.perform(patch("/api/v1/inspections/{inspectionId}/complete", inspectionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Inspection " + inspectionId + " was not found"));
    }

    @Test
    void recordsAFindingAndReturnsTheInspection() throws Exception {
        Inspection inspection = MaintenanceFixtures.inspectionWithFinding(FindingSeverity.HIGH);
        when(inspectionService.recordFinding(new RecordFindingCommand(inspection.getId(),
                "Diagonal crack of 3 mm in the beam of axis B-2", FindingSeverity.HIGH,
                MaintenanceFixtures.PHOTO_URL, null))).thenReturn(inspection);

        mockMvc.perform(post("/api/v1/inspections/{inspectionId}/findings", inspection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FINDING_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.findings[0].notes").value("Diagonal crack of 3 mm in the beam of axis B-2"))
                .andExpect(jsonPath("$.findings[0].recordedAt").value("2026-11-20T16:02:00Z"));
    }

    @Test
    void rejectsAFindingWithAnUnknownSeverityOrWithoutPhoto() throws Exception {
        UUID inspectionId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/inspections/{inspectionId}/findings", inspectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FINDING_REQUEST.replace("\"HIGH\"", "\"SEVERE\"")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/inspections/{inspectionId}/findings", inspectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Crack\",\"severity\":\"LOW\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.photoUrl").exists());

        verifyNoInteractions(inspectionService);
    }

    @Test
    void completesAnInspection() throws Exception {
        Inspection inspection = MaintenanceFixtures.inspectionWithFinding(FindingSeverity.LOW);
        inspection.complete(MaintenanceFixtures.NOW.plusSeconds(3600));
        when(inspectionService.completeInspection(inspection.getId())).thenReturn(inspection);

        mockMvc.perform(patch("/api/v1/inspections/{inspectionId}/complete", inspection.getId())
                        .header("X-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").value("2026-11-20T17:00:00Z"));

        verify(inspectionService).completeInspection(inspection.getId());
    }
}
