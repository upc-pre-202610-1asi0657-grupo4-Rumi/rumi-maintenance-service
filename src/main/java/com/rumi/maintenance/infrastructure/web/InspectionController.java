package com.rumi.maintenance.infrastructure.web;

import com.rumi.maintenance.application.InspectionApplicationService;
import com.rumi.maintenance.application.RecordFindingCommand;
import com.rumi.maintenance.application.ScheduleInspectionCommand;
import com.rumi.maintenance.domain.model.InspectionStatus;
import com.rumi.maintenance.infrastructure.web.dto.InspectionFindingRequestDto;
import com.rumi.maintenance.infrastructure.web.dto.InspectionResponseDto;
import com.rumi.maintenance.infrastructure.web.dto.ScheduleInspectionRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inspections")
@Tag(name = "Inspections", description = "Calendar of structural inspections and their findings; for administrators")
public class InspectionController {

    private static final String MANAGE_INSPECTIONS = "manage inspections";

    private final InspectionApplicationService inspectionService;
    private final InspectionDtoMapper mapper;

    public InspectionController(InspectionApplicationService inspectionService, InspectionDtoMapper mapper) {
        this.inspectionService = inspectionService;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Schedule an inspection",
            description = "Adds an inspection with its responsible engineer to the calendar of the building, with "
                    + "status PENDING (US17). The date must be today or later. When damageReportId is sent, the "
                    + "inspection evaluates that damage report, which moves to UNDER_REVIEW."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ScheduleInspectionRequestDto.class),
                    examples = @ExampleObject(name = "Inspection for a damage report",
                            value = OpenApiExamples.SCHEDULE_INSPECTION_REQUEST)
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Inspection scheduled as PENDING",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InspectionResponseDto.class),
                    examples = @ExampleObject(name = "Pending inspection",
                            value = OpenApiExamples.PENDING_INSPECTION_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "A field is missing or too long, the date is in the past, or the damage report belongs "
                    + "to another building",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Missing engineer", value = OpenApiExamples.VALIDATION_ERROR),
                            @ExampleObject(name = "Past date", value = OpenApiExamples.PAST_DATE_ERROR)
                    }
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "A resident tried to schedule an inspection",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Resident",
                            value = OpenApiExamples.RESIDENT_NOT_ADMINISTRATOR_ERROR)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The damage report does not exist",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InspectionResponseDto scheduleInspection(
            @Valid @RequestBody ScheduleInspectionRequestDto request,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.ROLE_HEADER, required = false) String role
    ) {
        GatewayUser.fromRole(role).requireAdministrator(MANAGE_INSPECTIONS);
        return mapper.toResponse(inspectionService.scheduleInspection(new ScheduleInspectionCommand(
                request.buildingId(), request.engineerName(), request.scheduledDate(), request.damageReportId())));
    }

    @Operation(
            summary = "List the inspections of a building",
            description = "Returns the calendar of inspections of the building ordered by date, with their findings "
                    + "(US17, US18). Filter by status to get, for example, only the PENDING ones."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Inspections of the building (possibly none)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = InspectionResponseDto.class)),
                    examples = @ExampleObject(name = "One pending inspection",
                            value = OpenApiExamples.INSPECTION_LIST_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "buildingId is missing or is not a valid UUID, or status is not PENDING, IN_PROGRESS, "
                    + "COMPLETED or CANCELLED",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Missing building id",
                            value = OpenApiExamples.MISSING_BUILDING_ID_ERROR)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "A resident asked for the calendar",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Resident",
                            value = OpenApiExamples.RESIDENT_NOT_ADMINISTRATOR_ERROR)
            )
    )
    @GetMapping
    public List<InspectionResponseDto> listInspections(
            @Parameter(description = "Identifier of the building", required = true,
                    example = "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d")
            @RequestParam UUID buildingId,
            @Parameter(description = "Only inspections with this status", example = "PENDING")
            @RequestParam(required = false) InspectionStatus status,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.ROLE_HEADER, required = false) String role
    ) {
        GatewayUser.fromRole(role).requireAdministrator(MANAGE_INSPECTIONS);
        return inspectionService.listInspections(buildingId, status).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Operation(
            summary = "Start an inspection",
            description = "The engineer starts the visit: a PENDING inspection moves to IN_PROGRESS, so its "
                    + "findings can be recorded (US18)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Inspection in progress",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InspectionResponseDto.class),
                    examples = @ExampleObject(name = "In progress",
                            value = OpenApiExamples.IN_PROGRESS_INSPECTION_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "A resident tried to start an inspection",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Resident",
                            value = OpenApiExamples.RESIDENT_NOT_ADMINISTRATOR_ERROR)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "No inspection has that identifier",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Unknown inspection",
                            value = OpenApiExamples.INSPECTION_NOT_FOUND_ERROR)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "The inspection is not PENDING",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Already started",
                            value = OpenApiExamples.INSPECTION_NOT_PENDING_ERROR)
            )
    )
    @PatchMapping("/{inspectionId}/start")
    public InspectionResponseDto startInspection(
            @Parameter(description = "Identifier of the inspection", example = "3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21")
            @PathVariable UUID inspectionId,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.ROLE_HEADER, required = false) String role
    ) {
        GatewayUser.fromRole(role).requireAdministrator(MANAGE_INSPECTIONS);
        return mapper.toResponse(inspectionService.startInspection(inspectionId));
    }

    @Operation(
            summary = "Record a finding of an inspection",
            description = "Records what the engineer found, with notes, severity and photo (US18). The finding is "
                    + "associated with the building of the inspection and receives the current date. The "
                    + "inspection must be IN_PROGRESS. Returns the inspection with all its findings."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InspectionFindingRequestDto.class),
                    examples = @ExampleObject(name = "Crack in a beam", value = OpenApiExamples.FINDING_REQUEST)
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Finding recorded",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InspectionResponseDto.class),
                    examples = @ExampleObject(name = "Inspection with one finding",
                            value = OpenApiExamples.INSPECTION_WITH_FINDING_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "A field is missing or too long, or the photo URL is not an http or https address",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "A resident tried to record a finding",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Resident",
                            value = OpenApiExamples.RESIDENT_NOT_ADMINISTRATOR_ERROR)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "No inspection has that identifier",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "The inspection is not IN_PROGRESS",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Not started",
                            value = OpenApiExamples.INSPECTION_NOT_IN_PROGRESS_ERROR)
            )
    )
    @PostMapping("/{inspectionId}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    public InspectionResponseDto recordFinding(
            @Parameter(description = "Identifier of the inspection", example = "3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21")
            @PathVariable UUID inspectionId,
            @Valid @RequestBody InspectionFindingRequestDto request,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.ROLE_HEADER, required = false) String role
    ) {
        GatewayUser.fromRole(role).requireAdministrator(MANAGE_INSPECTIONS);
        return mapper.toResponse(inspectionService.recordFinding(new RecordFindingCommand(
                inspectionId, request.notes(), request.severity(), request.photoUrl(), request.photoTakenAt())));
    }

    @Operation(
            summary = "Complete an inspection",
            description = "Ends the visit of an IN_PROGRESS inspection that has at least one finding."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Inspection completed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InspectionResponseDto.class),
                    examples = @ExampleObject(name = "Completed", value = OpenApiExamples.COMPLETED_INSPECTION_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "A resident tried to complete an inspection",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Resident",
                            value = OpenApiExamples.RESIDENT_NOT_ADMINISTRATOR_ERROR)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "No inspection has that identifier",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "The inspection is not IN_PROGRESS or has no findings",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "No findings",
                            value = OpenApiExamples.INSPECTION_WITHOUT_FINDINGS_ERROR)
            )
    )
    @PatchMapping("/{inspectionId}/complete")
    public InspectionResponseDto completeInspection(
            @Parameter(description = "Identifier of the inspection", example = "3e1f5a7c-2b4d-4c6e-8f0a-1b3d5f7a9c21")
            @PathVariable UUID inspectionId,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.ROLE_HEADER, required = false) String role
    ) {
        GatewayUser.fromRole(role).requireAdministrator(MANAGE_INSPECTIONS);
        return mapper.toResponse(inspectionService.completeInspection(inspectionId));
    }
}
