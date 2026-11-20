package com.rumi.maintenance.infrastructure.persistence;

import com.rumi.maintenance.domain.model.Inspection;
import com.rumi.maintenance.domain.model.InspectionFinding;
import com.rumi.maintenance.domain.model.InspectionStatus;
import com.rumi.maintenance.domain.model.PhotoEvidence;
import com.rumi.maintenance.domain.repository.InspectionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaInspectionRepository implements InspectionRepository {

    private final SpringDataInspectionRepository springDataRepository;

    public JpaInspectionRepository(SpringDataInspectionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Inspection> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Inspection> findByBuildingId(UUID buildingId, InspectionStatus status) {
        List<InspectionJpaEntity> entities = status == null
                ? springDataRepository.findByBuildingIdOrderByScheduledDateAscCreatedAtAsc(buildingId)
                : springDataRepository.findByBuildingIdAndStatusOrderByScheduledDateAscCreatedAtAsc(buildingId, status);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Inspection save(Inspection inspection) {
        return toDomain(springDataRepository.save(toEntity(inspection)));
    }

    private InspectionJpaEntity toEntity(Inspection inspection) {
        List<InspectionFindingJpaEntity> findings = inspection.getFindings().stream()
                .map(finding -> new InspectionFindingJpaEntity(
                        finding.getId(),
                        finding.getNotes(),
                        finding.getSeverity(),
                        finding.getEvidence().url(),
                        finding.getEvidence().takenAt(),
                        finding.getRecordedAt()))
                .toList();
        return new InspectionJpaEntity(
                inspection.getId(),
                inspection.getBuildingId(),
                inspection.getEngineerName(),
                inspection.getScheduledDate(),
                inspection.getStatus(),
                inspection.getCreatedAt(),
                inspection.getStartedAt(),
                inspection.getCompletedAt(),
                findings
        );
    }

    private Inspection toDomain(InspectionJpaEntity entity) {
        List<InspectionFinding> findings = entity.getFindings().stream()
                .map(finding -> new InspectionFinding(
                        finding.getId(),
                        finding.getNotes(),
                        finding.getSeverity(),
                        new PhotoEvidence(finding.getPhotoUrl(), finding.getPhotoTakenAt()),
                        finding.getRecordedAt()))
                .toList();
        return new Inspection(
                entity.getId(),
                entity.getBuildingId(),
                entity.getEngineerName(),
                entity.getScheduledDate(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                findings
        );
    }
}
