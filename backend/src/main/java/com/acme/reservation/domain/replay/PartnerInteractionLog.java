package com.acme.reservation.domain.replay;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Audit-safe record for partner request/response interactions.
 */
public class PartnerInteractionLog {

    private final String interactionId;
    private final String workItemId;
    private final String requestSnapshotRef;
    private final String responseSnapshotRef;
    private final Integer httpStatus;
    private final List<String> maskedFieldsApplied;
    private final String errorCode;
    private final Instant recordedAt;

    private PartnerInteractionLog(
            String interactionId,
            String workItemId,
            String requestSnapshotRef,
            String responseSnapshotRef,
            Integer httpStatus,
            List<String> maskedFieldsApplied,
            String errorCode,
            Instant recordedAt) {
        this.interactionId = interactionId;
        this.workItemId = Objects.requireNonNull(workItemId, "workItemId is required");
        this.requestSnapshotRef = Objects.requireNonNull(requestSnapshotRef, "requestSnapshotRef is required");
        this.responseSnapshotRef = responseSnapshotRef;
        this.httpStatus = httpStatus;
        this.maskedFieldsApplied = List.copyOf(maskedFieldsApplied);
        this.errorCode = errorCode;
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt is required");
    }

    public static PartnerInteractionLog create(
            String workItemId,
            String requestSnapshotRef,
            String responseSnapshotRef,
            Integer httpStatus,
            List<String> maskedFieldsApplied,
            String errorCode) {
        return new PartnerInteractionLog(
                UUID.randomUUID().toString(),
                workItemId,
                requestSnapshotRef,
                responseSnapshotRef,
                httpStatus,
                maskedFieldsApplied,
                errorCode,
                Instant.now());
    }

    public static Map<String, Object> operatorVisibleSnapshot(
            Map<String, Object> source,
            Set<String> sensitiveFields) {
        Map<String, Object> sanitized = new HashMap<>(source);
        for (String sensitive : sensitiveFields) {
            sanitized.remove(sensitive);
        }
        return sanitized;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public String getRequestSnapshotRef() {
        return requestSnapshotRef;
    }

    public String getResponseSnapshotRef() {
        return responseSnapshotRef;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public List<String> getMaskedFieldsApplied() {
        return maskedFieldsApplied;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
