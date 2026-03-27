package com.acme.foundation.adapters.inbound.http.filter;

import com.acme.foundation.application.ports.outbound.AuditEventRepository;
import com.acme.foundation.domain.audit.AuditEvent;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Publishes an audit event for every authenticated endpoint access and auth failure.
 * Runs after Spring Security filters so the security context is available.
 */
@Component
@Order(10)
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditEventRepository auditEventRepository;

    public AuditLoggingFilter(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        // Post-processing: record audit event after the response is committed
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String traceId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_KEY);
            if (traceId == null) {
                traceId = "unknown";
            }

            int status = response.getStatus();
            boolean authFailure = status == 401 || status == 403;

            // Only audit authenticated requests and auth failures
            if (auth != null && auth.isAuthenticated() && !authFailure) {
                AuditEvent event = AuditEvent.builder()
                        .actorId(auth.getName())
                        .actorType(AuditEvent.ActorType.USER)
                        .action("ENDPOINT_ACCESS")
                        .resourceType(request.getMethod())
                        .resourceId(request.getRequestURI())
                        .outcome(status < 400 ? AuditEvent.Outcome.SUCCESS : AuditEvent.Outcome.FAILURE)
                        .traceId(traceId)
                        .build();
                auditEventRepository.save(event);
            } else if (authFailure) {
                String actorId = (auth != null) ? auth.getName() : "anonymous";
                AuditEvent event = AuditEvent.builder()
                        .actorId(actorId)
                        .actorType(AuditEvent.ActorType.USER)
                        .action("AUTH_FAILURE")
                        .resourceId(request.getRequestURI())
                        .outcome(AuditEvent.Outcome.FAILURE)
                        .traceId(traceId)
                        .build();
                auditEventRepository.save(event);
            }
        } catch (Exception ex) {
            // Never fail the request due to audit logging
            logger.warn("Audit logging failed: " + ex.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }
}
