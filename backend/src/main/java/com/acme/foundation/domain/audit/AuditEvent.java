package com.acme.foundation.domain.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Security-relevant activity record.
 * Append-only — never mutated after creation.
 * GDPR-compliant: raw IP addresses are never stored.
 */
public final class AuditEvent {

    public enum ActorType {
        USER, SERVICE, SYSTEM
    }

    public enum Outcome {
        SUCCESS, FAILURE
    }

    private final String auditEventId;
    private final String actorId;
    private final ActorType actorType;
    private final String action;
    private final String resourceType;
    private final String resourceId;
    private final Outcome outcome;
    private final String traceId;
    private final Instant occurredAt;
    private final String ipAddressHash;

    private AuditEvent(Builder builder) {
        this.auditEventId = builder.auditEventId;
        this.actorId = builder.actorId;
        this.actorType = builder.actorType;
        this.action = builder.action;
        this.resourceType = builder.resourceType;
        this.resourceId = builder.resourceId;
        this.outcome = builder.outcome;
        this.traceId = builder.traceId;
        this.occurredAt = builder.occurredAt;
        this.ipAddressHash = builder.ipAddressHash;
    }

    public String getAuditEventId() {
        return auditEventId;
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

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
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

    public String getIpAddressHash() {
        return ipAddressHash;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String auditEventId = UUID.randomUUID().toString();
        private String actorId;
        private ActorType actorType = ActorType.USER;
        private String action;
        private String resourceType;
        private String resourceId;
        private Outcome outcome;
        private String traceId;
        private Instant occurredAt = Instant.now();
        private String ipAddressHash;

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder actorType(ActorType actorType) {
            this.actorType = actorType;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder outcome(Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder ipAddressHash(String ipAddressHash) {
            this.ipAddressHash = ipAddressHash;
            return this;
        }

        public AuditEvent build() {
            if (actorId == null || actorId.isBlank()) {
                throw new IllegalStateException("actorId is mandatory");
            }
            if (action == null || action.isBlank()) {
                throw new IllegalStateException("action is mandatory");
            }
            if (outcome == null) {
                throw new IllegalStateException("outcome is mandatory");
            }
            if (traceId == null || traceId.isBlank()) {
                throw new IllegalStateException("traceId is mandatory");
            }
            return new AuditEvent(this);
        }
    }
}
