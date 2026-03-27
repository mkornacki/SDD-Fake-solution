package com.acme.reservation.adapters.inbound.http.reservation;

import com.acme.reservation.application.ports.outbound.DlqRepository;
import com.acme.reservation.application.ports.outbound.GovernanceAuditEventRepository;
import com.acme.reservation.domain.audit.AuditEvent;
import com.acme.reservation.domain.audit.DlqItem;
import javax.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * T053: Admin DLQ replay controller.
 * POST /api/v1/operations/dlq/{dlqId}/replay requires admin:dlq scope.
 * An audit event is emitted on every successful replay request.
 */
@RestController
@RequestMapping("/api/v1/operations/dlq")
public class DlqController {

    private final DlqRepository dlqRepository;
    private final GovernanceAuditEventRepository governanceAuditEventRepository;

    public DlqController(
            DlqRepository dlqRepository,
            GovernanceAuditEventRepository governanceAuditEventRepository) {
        this.dlqRepository = dlqRepository;
        this.governanceAuditEventRepository = governanceAuditEventRepository;
    }

    public static class ReplayRequest {
        @NotBlank
        private String reason;

        public ReplayRequest() {
        }

        public ReplayRequest(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static final class ReplayResponse {
        private final String dlqId;
        private final String replayStatus;
        private final int replayCount;
        private final String message;

        public ReplayResponse(String dlqId, String replayStatus, int replayCount, String message) {
            this.dlqId = dlqId;
            this.replayStatus = replayStatus;
            this.replayCount = replayCount;
            this.message = message;
        }

        public String dlqId() {
            return dlqId;
        }

        public String getDlqId() {
            return dlqId;
        }

        public String replayStatus() {
            return replayStatus;
        }

        public String getReplayStatus() {
            return replayStatus;
        }

        public int replayCount() {
            return replayCount;
        }

        public int getReplayCount() {
            return replayCount;
        }

        public String message() {
            return message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * POST /api/v1/operations/dlq/{dlqId}/replay
     * Requires SCOPE_admin:dlq authority. Reason field is mandatory.
     */
    @PostMapping("/{dlqId}/replay")
    @PreAuthorize("hasAuthority('SCOPE_admin:dlq')")
    public ResponseEntity<ReplayResponse> replayDlqItem(
            @PathVariable String dlqId,
            @RequestBody ReplayRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        if (request == null || request.reason() == null || request.reason().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        DlqItem dlqItem = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new javax.persistence.EntityNotFoundException(
                        "DLQ item not found: " + dlqId));

        dlqItem.markInReview();
        dlqItem.markReplayed();
        dlqRepository.save(dlqItem);

        String actorId = resolveActorId(jwt, authentication);
        String traceId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        Instant now = Instant.now();

        AuditEvent auditEvent = AuditEvent.builder()
                .entityType("DLQ_ITEM")
                .entityId(dlqItem.getDlqId())
                .actorId(actorId)
                .actorType(AuditEvent.ActorType.USER)
                .action("DLQ_REPLAY_REQUESTED")
                .outcome(AuditEvent.Outcome.SUCCESS)
                .traceId(traceId)
                .occurredAt(now)
                .afterRef("reason=" + request.reason())
                .build();
        governanceAuditEventRepository.append(auditEvent);

        return ResponseEntity.accepted().body(new ReplayResponse(
                dlqItem.getDlqId(),
                dlqItem.getReplayStatus().name(),
                dlqItem.getReplayCount(),
                "DLQ item scheduled for replay"));
    }

    private String resolveActorId(Jwt jwt, Authentication authentication) {
        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return jwt.getSubject();
        }
        if (authentication != null && authentication.getName() != null
                && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return "admin";
    }
}
