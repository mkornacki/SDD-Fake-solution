package com.acme.reservation.adapters.outbound.audit;

import com.acme.reservation.application.ports.outbound.GovernanceAuditEventRepository;
import com.acme.reservation.domain.audit.AuditEvent;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("reservationAuditEventJpaAdapter")
public class AuditEventJpaAdapter implements GovernanceAuditEventRepository {

    private final AuditEventSpringDataRepository repository;

    public AuditEventJpaAdapter(AuditEventSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditEvent append(AuditEvent event) {
        GovernanceAuditEventJpaEntity saved = repository.save(
                GovernanceAuditEventJpaEntity.from(event));
        return saved.toDomain();
    }

    @Override
    public List<AuditEvent> findByEntityIdAndType(String entityId, String entityType) {
        return repository.findByEntityIdAndEntityTypeOrderByOccurredAtAsc(entityId, entityType)
                .stream()
                .map(GovernanceAuditEventJpaEntity::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}