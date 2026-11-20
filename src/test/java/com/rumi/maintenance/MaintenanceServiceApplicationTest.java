package com.rumi.maintenance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumi.maintenance.infrastructure.seed.DevDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the whole service with the dev profile (H2 and sample data) and runs the maintenance flows end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MaintenanceServiceApplicationTest {

    private static final String BUILDING_ID = DevDataSeeder.BUILDING_ID.toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @Test
    void contextLoadsAndTheHealthEndpointAnswers() throws Exception {
        mockMvc.perform(get("/api/v1/inspections/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("rumi-maintenance-service"));
    }

    @Test
    void theSampleInspectionsAndTheDamageBoardAreAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/inspections").param("buildingId", BUILDING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(DevDataSeeder.PENDING_INSPECTION_ID.toString())))
                .andExpect(jsonPath("$[*].id").value(hasItem(DevDataSeeder.COMPLETED_INSPECTION_ID.toString())));
        mockMvc.perform(get("/api/v1/damage-reports/board").param("buildingId", BUILDING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zones[0].zone").value("FLOOR-2-EAST"))
                .andExpect(jsonPath("$.zones[0].highestSeverity").value("HIGH"));
    }

    @Test
    void anInspectionIsScheduledForADamageReportAndRunUntilItIsCompleted() throws Exception {
        String report = mockMvc.perform(post("/api/v1/damage-reports")
                        .header("X-Role", "RESIDENT")
                        .header("X-Building-Id", BUILDING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingId\":\"" + BUILDING_ID + "\",\"description\":\"Crack over the door\","
                                + "\"zone\":\"FLOOR-5-WEST\",\"photoUrl\":\"https://storage.example.com/a.jpg\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andReturn().getResponse().getContentAsString();
        String reportId = objectMapper.readTree(report).get("id").asText();

        String scheduled = mockMvc.perform(post("/api/v1/inspections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingId\":\"" + BUILDING_ID + "\",\"engineerName\":\"Ing. Ana Ruiz\","
                                + "\"scheduledDate\":\"" + LocalDate.now(clock) + "\",\"damageReportId\":\""
                                + reportId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String inspectionId = objectMapper.readTree(scheduled).get("id").asText();

        mockMvc.perform(get("/api/v1/damage-reports/board").param("buildingId", BUILDING_ID))
                .andExpect(jsonPath("$.zones[*].zone").value(hasItem("FLOOR-5-WEST")));
        mockMvc.perform(patch("/api/v1/inspections/{id}/start", inspectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(patch("/api/v1/inspections/{id}/complete", inspectionId))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/inspections/{id}/findings", inspectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Crack of 1 mm in plaster only\",\"severity\":\"LOW\","
                                + "\"photoUrl\":\"https://storage.example.com/b.jpg\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.findings[0].severity").value("LOW"));
        mockMvc.perform(patch("/api/v1/inspections/{id}/complete", inspectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.highestSeverity").value("LOW"));
        mockMvc.perform(patch("/api/v1/damage-reports/{id}/review", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"LOW\",\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.generatedInspectionId").value(inspectionId));
    }

    @Test
    void theOpenApiDocumentDescribesTheMaintenanceEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/inspections'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/inspections'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/inspections/{inspectionId}/start'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/inspections/{inspectionId}/findings'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/inspections/{inspectionId}/complete'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/damage-reports'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/damage-reports/board'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/damage-reports/{reportId}/review'].patch").exists());
    }
}
