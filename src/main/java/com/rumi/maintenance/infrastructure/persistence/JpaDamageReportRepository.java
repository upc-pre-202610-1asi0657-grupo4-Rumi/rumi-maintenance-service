package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.domain.model.DamageReport;
import com.rumi.maintenance.domain.model.DamageReportStatus;
import com.rumi.maintenance.domain.model.PhotoEvidence;
import com.rumi.maintenance.domain.repository.DamageReportRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDamageReportRepository implements DamageReportRepository {

    private final SpringDataDamageReportRepository springDataRepository;

    public JpaDamageReportRepository(SpringDataDamageReportRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<DamageReport> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DamageReport> findByBuildingId(UUID buildingId, boolean includeResolved) {
        List<DamageReportJpaEntity> entities = includeResolved
                ? springDataRepository.findByBuildingIdOrderByReportedAtDescIdAsc(buildingId)
                : springDataRepository.findByBuildingIdAndStatusNotOrderByReportedAtDescIdAsc(
                        buildingId, DamageReportStatus.RESOLVED);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public DamageReport save(DamageReport report) {
        return toDomain(springDataRepository.save(toEntity(report)));
    }

    private DamageReportJpaEntity toEntity(DamageReport report) {
        return new DamageReportJpaEntity(
                report.getId(),
                report.getBuildingId(),
                report.getResidentUserId(),
                report.getDescription(),
                report.getZone(),
                report.getEvidence().url(),
                report.getEvidence().takenAt(),
                report.getSeverity(),
                report.getStatus(),
                report.getGeneratedInspectionId(),
                report.getReportedAt(),
                report.getReviewedAt(),
                report.getResolvedAt()
        );
    }

    private DamageReport toDomain(DamageReportJpaEntity entity) {
        return new DamageReport(
                entity.getId(),
                entity.getBuildingId(),
                entity.getResidentUserId(),
                entity.getDescription(),
                entity.getZone(),
                new PhotoEvidence(entity.getPhotoUrl(), entity.getPhotoTakenAt()),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getGeneratedInspectionId(),
                entity.getReportedAt(),
                entity.getReviewedAt(),
                entity.getResolvedAt()
        );
    }
}
