package com.rumi.maintenance.application;

import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.PhotoEvidence;
import com.rumi.maintenance.domain.repository.DamageReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DamageReportApplicationService {

    private final DamageReportRepository damageReportRepository;
    private final Clock clock;

    public DamageReportApplicationService(DamageReportRepository damageReportRepository, Clock clock) {
        this.damageReportRepository = damageReportRepository;
        this.clock = clock;
    }

    /**
     * Registers a damage reported by a resident with its photo and zone (US21). The report becomes visible
     * on the damage board of the administrator.
     */
    @Transactional
    public DamageReport reportDamage(ReportDamageCommand command) {
        if (command.buildingId() == null) {
            throw new IllegalArgumentException("Building id is required");
        }
        return damageReportRepository.save(DamageReport.report(
                UUID.randomUUID(),
                command.buildingId(),
                command.residentUserId(),
                command.description(),
                command.zone(),
                new PhotoEvidence(command.photoUrl(), command.photoTakenAt()),
                clock.instant()
        ));
    }

    /**
     * Damage board of a building grouped by zone and severity (US22). Resolved reports are left out unless
     * {@code includeResolved} is true.
     */
    public DamageBoard getBoard(UUID buildingId, boolean includeResolved) {
        if (buildingId == null) {
            throw new IllegalArgumentException("Building id is required");
        }
        return DamageBoard.of(buildingId, damageReportRepository.findByBuildingId(buildingId, includeResolved));
    }

    public DamageReport getDamageReport(UUID reportId) {
        return damageReportRepository.findById(reportId)
                .orElseThrow(() -> new DamageReportNotFoundException(reportId));
    }

    /**
     * The administrator assigns the severity of a damage report and keeps it UNDER_REVIEW or marks it
     * RESOLVED, so the board shows what to fix first (US22).
     */
    @Transactional
    public DamageReport reviewDamageReport(ReviewDamageReportCommand command) {
        if (command.severity() == null) {
            throw new IllegalArgumentException("Severity is required");
        }
        DamageReport report = getDamageReport(command.reportId());
        DamageReportStatus target = command.status() == null ? DamageReportStatus.UNDER_REVIEW : command.status();
        switch (target) {
            case UNDER_REVIEW -> report.review(command.severity(), clock.instant());
            case RESOLVED -> report.resolve(command.severity(), clock.instant());
            case REPORTED -> throw new IllegalArgumentException(
                    "A reviewed damage report can only be UNDER_REVIEW or RESOLVED");
        }
        return damageReportRepository.save(report);
    }
}
