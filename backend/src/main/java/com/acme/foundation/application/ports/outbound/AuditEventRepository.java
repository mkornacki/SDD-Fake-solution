package com.acme.foundation.application.ports.outbound;

import com.acme.foundation.domain.audit.AuditEvent;

/**
 * Outbound port for persisting audit events.
 * Append-only — no update or delete operations exposed.
 */
public interface AuditEventRepository {

    /**
     * Persist an audit event record.
     *
     * @param event the event to store
     */
    void save(AuditEvent event);
}
