package com.acme.reservation.adapters.outbound.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventSpringDataRepository
        extends JpaRepository<GovernanceAuditEventJpaEntity, String> {

    List<GovernanceAuditEventJpaEntity> findByEntityIdAndEntityTypeOrderByOccurredAtAsc(
            String entityId,
            String entityType);

    List<GovernanceAuditEventJpaEntity> findByTraceId(String traceId);
}