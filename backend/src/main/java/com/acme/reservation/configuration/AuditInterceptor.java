package com.acme.reservation.configuration;

import com.acme.reservation.application.ports.outbound.GovernanceAuditEventRepository;
import com.acme.reservation.domain.audit.AuditEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Locale;

/**
 * AOP interceptor for append-only governance audit events.
 */
@Aspect
@Component
public class AuditInterceptor {

    private final GovernanceAuditEventRepository governanceAuditEventRepository;

    public AuditInterceptor(GovernanceAuditEventRepository governanceAuditEventRepository) {
        this.governanceAuditEventRepository = governanceAuditEventRepository;
    }

    @Around("execution(* com.acme.reservation.application.command..*CommandHandler.execute(..))")
    public Object aroundCommandExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String actorId = extractStringProperty(firstArg(joinPoint), "actorId", "system");
        String traceId = extractStringProperty(firstArg(joinPoint), "traceId", "trace-unavailable");
        String commandEntityId = extractStringProperty(
                firstArg(joinPoint),
                "reservationId",
                extractStringProperty(firstArg(joinPoint), "idempotencyKey", "unknown"));
        String action = resolveAction(joinPoint);

        try {
            Object result = joinPoint.proceed();
            String resultEntityId = extractStringProperty(result, "reservationId", commandEntityId);
            appendEvent(resultEntityId, actorId, traceId, action, AuditEvent.Outcome.SUCCESS);
            return result;
        } catch (Throwable throwable) {
            appendEvent(commandEntityId, actorId, traceId, action, AuditEvent.Outcome.FAILURE);
            throw throwable;
        }
    }

    private void appendEvent(
            String entityId,
            String actorId,
            String traceId,
            String action,
            AuditEvent.Outcome outcome) {
        AuditEvent event = AuditEvent.builder()
                .entityType("RESERVATION")
                .entityId(entityId == null || entityId.isBlank() ? "unknown" : entityId)
                .actorId(actorId == null || actorId.isBlank() ? "system" : actorId)
                .actorType(AuditEvent.ActorType.USER)
                .action(action)
                .outcome(outcome)
                .traceId(traceId == null || traceId.isBlank() ? "trace-unavailable" : traceId)
                .occurredAt(Instant.now())
                .build();
        governanceAuditEventRepository.append(event);
    }

    private Object firstArg(ProceedingJoinPoint joinPoint) {
        return joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : null;
    }

    private String resolveAction(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String normalized = className.replace("CommandHandler", "")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "UNKNOWN_ACTION" : normalized;
    }

    private String extractStringProperty(Object object, String methodName, String fallback) {
        if (object == null) {
            return fallback;
        }
        try {
            Method method = object.getClass().getMethod(methodName);
            Object value = method.invoke(object);
            if (value == null) {
                return fallback;
            }
            String stringValue = value.toString();
            return stringValue.isBlank() ? fallback : stringValue;
        } catch (ReflectiveOperationException e) {
            return fallback;
        }
    }
}