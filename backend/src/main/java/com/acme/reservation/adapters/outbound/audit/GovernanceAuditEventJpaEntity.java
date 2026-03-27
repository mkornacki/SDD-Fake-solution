package com.acme.reservation.adapters.outbound.audit;

import com.acme.reservation.domain.audit.AuditEvent;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reservation_audit_events")
public class GovernanceAuditEventJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "outcome", nullable = false)
    private String outcome;

    @Column(name = "before_snapshot_ref")
    private String beforeSnapshotRef;

    @Column(name = "after_snapshot_ref")
    private String afterSnapshotRef;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static GovernanceAuditEventJpaEntity from(AuditEvent event) {
        GovernanceAuditEventJpaEntity entity = new GovernanceAuditEventJpaEntity();
        entity.eventId = event.getAuditEventId();
        entity.entityId = event.getEntityId();
        entity.entityType = event.getEntityType();
        entity.actorId = event.getActorId();
        entity.actorType = event.getActorType().name();
        entity.action = event.getAction();
        entity.outcome = event.getOutcome().name();
        entity.beforeSnapshotRef = event.getBeforeRef();
        entity.afterSnapshotRef = event.getAfterRef();
        entity.traceId = event.getTraceId();
        entity.occurredAt = event.getOccurredAt();
        return entity;
    }

    public AuditEvent toDomain() {
        return AuditEvent.builder()
                .auditEventId(eventId)
                .entityId(entityId)
                .entityType(entityType)
                .actorId(actorId)
                .actorType(AuditEvent.ActorType.valueOf(actorType))
                .action(action)
                .outcome(AuditEvent.Outcome.valueOf(outcome))
                .beforeRef(beforeSnapshotRef)
                .afterRef(afterSnapshotRef)
                .traceId(traceId)
                .occurredAt(occurredAt)
                .build();
    }

    public String getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getTraceId() {
        return traceId;
    }
}