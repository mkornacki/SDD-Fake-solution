package com.acme.reservation.application.ports.outbound;

import com.acme.reservation.domain.audit.AuditEvent;

import java.util.List;

/**
 * Outbound port for immutable governance-grade audit events.
 */
public interface GovernanceAuditEventRepository {

    AuditEvent append(AuditEvent event);

    List<AuditEvent> findByEntityIdAndType(String entityId, String entityType);
}