package com.acme.foundation.adapters.outbound.persistence;

import com.acme.foundation.application.ports.outbound.AuditEventRepository;
import com.acme.foundation.domain.audit.AuditEvent;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * JPA adapter for the AuditEventRepository outbound port.
 */
@Repository
public class AuditEventJpaAdapter implements AuditEventRepository {

    private final AuditEventJpaRepository jpaRepository;

    public AuditEventJpaAdapter(AuditEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(AuditEvent event) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setAuditEventId(event.getAuditEventId());
        entity.setActorId(event.getActorId());
        entity.setActorType(event.getActorType().name());
        entity.setAction(event.getAction());
        entity.setResourceType(event.getResourceType());
        entity.setResourceId(event.getResourceId());
        entity.setOutcome(event.getOutcome().name());
        entity.setTraceId(event.getTraceId());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setIpAddressHash(event.getIpAddressHash());
        jpaRepository.save(entity);
    }

    @Entity
    @Table(name = "audit_events")
    static class AuditEventEntity {

        @Id
        @Column(name = "audit_event_id")
        private String auditEventId;

        @Column(name = "actor_id", nullable = false)
        private String actorId;

        @Column(name = "actor_type", nullable = false)
        private String actorType;

        @Column(name = "action", nullable = false)
        private String action;

        @Column(name = "resource_type")
        private String resourceType;

        @Column(name = "resource_id")
        private String resourceId;

        @Column(name = "outcome", nullable = false)
        private String outcome;

        @Column(name = "trace_id", nullable = false)
        private String traceId;

        @Column(name = "occurred_at", nullable = false)
        private Instant occurredAt;

        @Column(name = "ip_address_hash")
        private String ipAddressHash;

        public void setAuditEventId(String auditEventId) {
            this.auditEventId = auditEventId;
        }

        public void setActorId(String actorId) {
            this.actorId = actorId;
        }

        public void setActorType(String actorType) {
            this.actorType = actorType;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public void setOutcome(String outcome) {
            this.outcome = outcome;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public void setOccurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
        }

        public void setIpAddressHash(String ipAddressHash) {
            this.ipAddressHash = ipAddressHash;
        }

        public String getAuditEventId() {
            return auditEventId;
        }
    }
}
