package com.rumi.maintenance.infrastructure.web;

import com.rumi.maintenance.application.DamageReportApplicationService;
import com.rumi.maintenance.application.ReportDamageCommand;
import com.rumi.maintenance.application.ReviewDamageReportCommand;
import com.rumi.maintenance.infrastructure.web.dto.DamageBoardResponseDto;
import com.rumi.maintenance.infrastructure.web.dto.DamageReportRequestDto;
import com.rumi.maintenance.infrastructure.web.dto.DamageReportResponseDto;
import com.rumi.maintenance.infrastructure.web.dto.DamageReviewRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/damage-reports")
@Tag(name = "Damage reports", description = "Damage reported by residents and the damage board of the administrator")
public class DamageReportController {

    private static final String MANAGE_DAMAGE_BOARD = "manage the damage board";

    private final DamageReportApplicationService damageReportService;
    private final DamageReportDtoMapper mapper;

    public DamageReportController(DamageReportApplicationService damageReportService, DamageReportDtoMapper mapper) {
        this.damageReportService = damageReportService;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Report a damage",
            description = "A resident reports a visible damage with its photo and zone (US21). The report is "
                    + "registered as REPORTED, without severity, and is visible on the damage board of the "
                    + "administrator. Through the gateway, a RESIDENT can only report damage in the buildings of "
                    + "their token; the reporter comes from the X-User-Id header."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DamageReportRequestDto.class),
                    examples = @ExampleObject(name = "Crack in a wall", value = OpenApiExamples.DAMAGE_REPORT_REQUEST)
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Damage report registered",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DamageReportResponseDto.class),
                    examples = @ExampleObject(name = "New report", value = OpenApiExamples.DAMAGE_REPORT_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "A field is missing or too long, or the photo URL is not an http or https address",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Invalid photo URL", value = OpenApiExamples.PHOTO_URL_ERROR)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "A resident tried to report damage in a building that is not in their token",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Other building",
                            value = OpenApiExamples.RESIDENT_OTHER_BUILDING_ERROR)
            )
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DamageReportResponseDto reportDamage(
            @Valid @RequestBody DamageReportRequestDto request,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.ROLE_HEADER, required = false) String role,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.BUILDING_IDS_HEADER, required = false)
            String buildingIds,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.USER_ID_HEADER, required = false)
            String userId
    ) {
        GatewayUser.from(role, buildingIds).requireAccessTo(request.buildingId());
        return mapper.toResponse(damageReportService.reportDamage(new ReportDamageCommand(
                request.buildingId(),
                GatewayUser.parseUserId(userId),
                request.description(),
                request.zone(),
                request.photoUrl(),
                request.photoTakenAt()
        )));
    }

    @Operation(
            summary = "Get the damage board of a building",
            description = "Returns the damage reports of the building grouped by zone and, inside each zone, by "
                    + "severity, so the administrator can prioritize corrective actions (US22). Zones with the most "
                    + "severe damage come first; reports not reviewed yet appear as UNASSESSED. Resolved reports are "
                    + "left out unless includeResolved is true."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Damage board (possibly with no zones)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DamageBoardResponseDto.class),
                    examples = @ExampleObject(name = "Two zones", value = OpenApiExamples.DAMAGE_BOARD_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "buildingId is missing or is not a valid UUID",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "A resident asked for the damage board",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @GetMapping("/board")
    public DamageBoardResponseDto getBoard(
            @Parameter(description = "Identifier of the building", required = true,
                    example = "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d")
            @RequestParam UUID buildingId,
            @Parameter(description = "Also show resolved reports", example = "false")
            @RequestParam(defaultValue = "false") boolean includeResolved,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.ROLE_HEADER, required = false) String role
    ) {
        GatewayUser.fromRole(role).requireAdministrator(MANAGE_DAMAGE_BOARD);
        return mapper.toResponse(damageReportService.getBoard(buildingId, includeResolved));
    }

    @Operation(
            summary = "Review a damage report",
            description = "The administrator assigns the severity of the damage and keeps the report UNDER_REVIEW or "
                    + "marks it RESOLVED (US22). A resolved report cannot change any more."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DamageReviewRequestDto.class),
                    examples = @ExampleObject(name = "High severity", value = OpenApiExamples.DAMAGE_REVIEW_REQUEST)
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Damage report reviewed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DamageReportResponseDto.class),
                    examples = @ExampleObject(name = "Under review",
                            value = OpenApiExamples.REVIEWED_DAMAGE_REPORT_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "severity or status is missing, or status is REPORTED",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "A resident tried to review a damage report",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "No damage report has that identifier",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Unknown report",
                            value = OpenApiExamples.DAMAGE_REPORT_NOT_FOUND_ERROR)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "The damage report is already RESOLVED",
            content = @Content(
                    mediaType = OpenApiExamples.PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Already resolved",
                            value = OpenApiExamples.DAMAGE_REPORT_RESOLVED_ERROR)
            )
    )
    @PatchMapping("/{reportId}/review")
    public DamageReportResponseDto reviewDamageReport(
            @Parameter(description = "Identifier of the damage report", example = "8c2e4a6f-1b3d-4f5a-9c7e-2d4f6a8c0e13")
            @PathVariable UUID reportId,
            @Valid @RequestBody DamageReviewRequestDto request,
            @Parameter(hidden = true) @RequestHeader(value = GatewayUser.ROLE_HEADER, required = false) String role
    ) {
        GatewayUser.fromRole(role).requireAdministrator(MANAGE_DAMAGE_BOARD);
        return mapper.toResponse(damageReportService.reviewDamageReport(
                new ReviewDamageReportCommand(reportId, request.severity(), request.status())));
    }
}
