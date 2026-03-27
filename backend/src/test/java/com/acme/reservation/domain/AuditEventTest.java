package com.acme.reservation.domain;

import com.acme.reservation.domain.audit.AuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("T033: AuditEvent unit tests")
class AuditEventTest {

    @Test
    @DisplayName("AuditEvent is immutable and has no setter methods")
    void auditEvent_isImmutable() {
        Method[] methods = AuditEvent.class.getDeclaredMethods();
        long setters = java.util.Arrays.stream(methods)
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .count();

        assertThat(setters).isZero();
    }

    @Test
    @DisplayName("AuditEvent enforces mandatory fields at construction")
    void auditEvent_requiresMandatoryFields() {
        assertThatThrownBy(() -> AuditEvent.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mandatory");

        AuditEvent event = AuditEvent.builder()
                .entityId("res-123")
                .entityType("RESERVATION")
                .actorId("user-1")
                .actorType(AuditEvent.ActorType.USER)
                .action("CREATE_RESERVATION")
                .outcome(AuditEvent.Outcome.SUCCESS)
                .traceId("trace-123")
                .build();

        assertThat(event.getAuditEventId()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getActorId()).isEqualTo("user-1");
    }
}