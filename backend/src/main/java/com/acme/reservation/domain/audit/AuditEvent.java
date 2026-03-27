package com.acme.reservation.domain.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable compliance-grade audit event.
 */
public final class AuditEvent {

    public enum ActorType {
        USER,
        SYSTEM,
        SERVICE
    }

    public enum Outcome {
        SUCCESS,
        FAILURE,
        PARTIAL
    }

    private final String auditEventId;
    private final String entityType;
    private final String entityId;
    private final String actorId;
    private final ActorType actorType;
    private final String action;
    private final Outcome outcome;
    private final String traceId;
    private final Instant occurredAt;
    private final String beforeRef;
    private final String afterRef;

    private AuditEvent(Builder builder) {
        this.auditEventId = builder.auditEventId;
        this.entityType = builder.entityType;
        this.entityId = builder.entityId;
        this.actorId = builder.actorId;
        this.actorType = builder.actorType;
        this.action = builder.action;
        this.outcome = builder.outcome;
        this.traceId = builder.traceId;
        this.occurredAt = builder.occurredAt;
        this.beforeRef = builder.beforeRef;
        this.afterRef = builder.afterRef;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAuditEventId() {
        return auditEventId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getActorId() {
        return actorId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public String getAction() {
        return action;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getBeforeRef() {
        return beforeRef;
    }

    public String getAfterRef() {
        return afterRef;
    }

    public static final class Builder {
        private String auditEventId = UUID.randomUUID().toString();
        private String entityType;
        private String entityId;
        private String actorId;
        private ActorType actorType;
        private String action;
        private Outcome outcome;
        private String traceId;
        private Instant occurredAt = Instant.now();
        private String beforeRef;
        private String afterRef;

        public Builder auditEventId(String value) {
            this.auditEventId = value;
            return this;
        }

        public Builder entityType(String value) {
            this.entityType = value;
            return this;
        }

        public Builder entityId(String value) {
            this.entityId = value;
            return this;
        }

        public Builder actorId(String value) {
            this.actorId = value;
            return this;
        }

        public Builder actorType(ActorType value) {
            this.actorType = value;
            return this;
        }

        public Builder action(String value) {
            this.action = value;
            return this;
        }

        public Builder outcome(Outcome value) {
            this.outcome = value;
            return this;
        }

        public Builder traceId(String value) {
            this.traceId = value;
            return this;
        }

        public Builder occurredAt(Instant value) {
            this.occurredAt = value;
            return this;
        }

        public Builder beforeRef(String value) {
            this.beforeRef = value;
            return this;
        }

        public Builder afterRef(String value) {
            this.afterRef = value;
            return this;
        }

        public AuditEvent build() {
            if (isBlank(entityType)) {
                throw new IllegalStateException("entityType is mandatory");
            }
            if (isBlank(entityId)) {
                throw new IllegalStateException("entityId is mandatory");
            }
            if (isBlank(actorId)) {
                throw new IllegalStateException("actorId is mandatory");
            }
            if (actorType == null) {
                throw new IllegalStateException("actorType is mandatory");
            }
            if (isBlank(action)) {
                throw new IllegalStateException("action is mandatory");
            }
            if (outcome == null) {
                throw new IllegalStateException("outcome is mandatory");
            }
            if (isBlank(traceId)) {
                throw new IllegalStateException("traceId is mandatory");
            }
            if (occurredAt == null) {
                throw new IllegalStateException("occurredAt is mandatory");
            }
            return new AuditEvent(this);
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}