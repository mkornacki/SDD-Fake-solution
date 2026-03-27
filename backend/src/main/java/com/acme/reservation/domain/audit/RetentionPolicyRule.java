package com.acme.reservation.domain.audit;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Domain policy governing retention behavior by artifact class.
 */
public final class RetentionPolicyRule {

    public enum ArtifactClass {
        RAW_PAYLOAD,
        REQUEST_SNAPSHOT,
        RESPONSE_SNAPSHOT,
        AUDIT_EVENT
    }

    public enum Disposition {
        DELETE,
        ARCHIVE,
        RETAIN
    }

    private final String ruleId;
    private final ArtifactClass artifactClass;
    private final Disposition disposition;
    private final int retentionDays;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;

    public RetentionPolicyRule(
            String ruleId,
            ArtifactClass artifactClass,
            Disposition disposition,
            int retentionDays,
            String description,
            Instant createdAt,
            Instant updatedAt) {
        if (artifactClass == null) {
            throw new IllegalArgumentException("artifactClass is required");
        }
        if (disposition == null) {
            throw new IllegalArgumentException("disposition is required");
        }
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("retentionDays must be positive");
        }
        this.ruleId = ruleId == null || ruleId.isBlank() ? UUID.randomUUID().toString() : ruleId;
        this.artifactClass = artifactClass;
        this.disposition = disposition;
        this.retentionDays = retentionDays;
        this.description = description;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public static RetentionPolicyRule defaultRuleFor(ArtifactClass artifactClass) {
        switch (artifactClass) {
            case RAW_PAYLOAD:
                return new RetentionPolicyRule(
                        null,
                        ArtifactClass.RAW_PAYLOAD,
                        Disposition.DELETE,
                        30,
                        "Delete raw payloads after retention window",
                        Instant.now(),
                        Instant.now());
            case REQUEST_SNAPSHOT:
                return new RetentionPolicyRule(
                        null,
                        ArtifactClass.REQUEST_SNAPSHOT,
                        Disposition.DELETE,
                        30,
                        "Delete request snapshots after retention window",
                        Instant.now(),
                        Instant.now());
            case RESPONSE_SNAPSHOT:
                return new RetentionPolicyRule(
                        null,
                        ArtifactClass.RESPONSE_SNAPSHOT,
                        Disposition.DELETE,
                        30,
                        "Delete response snapshots after retention window",
                        Instant.now(),
                        Instant.now());
            case AUDIT_EVENT:
                return new RetentionPolicyRule(
                        null,
                        ArtifactClass.AUDIT_EVENT,
                        Disposition.ARCHIVE,
                        365,
                        "Archive audit events for governance retention",
                        Instant.now(),
                        Instant.now());
            default:
                throw new IllegalArgumentException("Unsupported artifact class: " + artifactClass);
        }
    }

    public String getRuleId() {
        return ruleId;
    }

    public ArtifactClass getArtifactClass() {
        return artifactClass;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Application service for evaluating retention actions on aged artifacts.
     */
    public static final class RetentionPolicyExecutor {

        public List<RetentionDecision> evaluate(
                List<RetentionArtifact> artifacts,
                List<RetentionPolicyRule> rules,
                Instant now) {
            Map<ArtifactClass, RetentionPolicyRule> rulesByClass = rules.stream()
                    .collect(Collectors.toMap(
                            RetentionPolicyRule::getArtifactClass,
                            rule -> rule,
                            (left, right) -> right));

            return artifacts.stream()
                    .filter(artifact -> {
                        RetentionPolicyRule rule = rulesByClass.get(artifact.artifactClass());
                        if (rule == null) {
                            return false;
                        }
                        Instant expiry = artifact.createdAt().plusSeconds((long) rule.retentionDays * 24 * 60 * 60);
                        return !expiry.isAfter(now);
                    })
                    .map(artifact -> {
                        RetentionPolicyRule rule = rulesByClass.get(artifact.artifactClass());
                        return new RetentionDecision(artifact.artifactId(), artifact.artifactClass(), rule.disposition);
                    })
                    .collect(Collectors.toList());
        }
    }

    public static final class RetentionArtifact {
        private final String artifactId;
        private final ArtifactClass artifactClass;
        private final Instant createdAt;

        public RetentionArtifact(String artifactId, ArtifactClass artifactClass, Instant createdAt) {
            this.artifactId = artifactId;
            this.artifactClass = artifactClass;
            this.createdAt = createdAt;
        }

        public String artifactId() {
            return artifactId;
        }

        public ArtifactClass artifactClass() {
            return artifactClass;
        }

        public Instant createdAt() {
            return createdAt;
        }
    }

    public static final class RetentionDecision {
        private final String artifactId;
        private final ArtifactClass artifactClass;
        private final Disposition disposition;

        public RetentionDecision(String artifactId, ArtifactClass artifactClass, Disposition disposition) {
            this.artifactId = artifactId;
            this.artifactClass = artifactClass;
            this.disposition = disposition;
        }

        public String artifactId() {
            return artifactId;
        }

        public ArtifactClass artifactClass() {
            return artifactClass;
        }

        public Disposition disposition() {
            return disposition;
        }
    }
}
