package com.acme.reservation.domain;

import com.acme.reservation.domain.replay.PartnerInteractionLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("T023: Partner interaction masking")
class PartnerLogMaskingTest {

    @Test
    @DisplayName("Operator-visible snapshot excludes sensitive fields")
    void operatorSnapshot_excludesSensitiveFields() {
        Map<String, Object> payload = Map.of(
                "guestName", "Jane Doe",
                "email", "jane@example.com",
                "phone", "+15551234567",
                "roomCode", "RM-101"
        );

        Set<String> sensitive = Set.of("email", "phone");

        Map<String, Object> masked = PartnerInteractionLog.operatorVisibleSnapshot(payload, sensitive);

        assertThat(masked).containsEntry("guestName", "Jane Doe");
        assertThat(masked).containsEntry("roomCode", "RM-101");
        assertThat(masked).doesNotContainKeys("email", "phone");
    }

    @Test
    @DisplayName("Masked fields metadata captured in interaction log")
    void log_recordsMaskedFieldList() {
        PartnerInteractionLog log = PartnerInteractionLog.create(
                "work-1",
                "snap:req:1",
                "snap:res:1",
                503,
                List.of("email", "phone"),
                "PARTNER_TIMEOUT"
        );

        assertThat(log.getMaskedFieldsApplied()).containsExactlyInAnyOrder("email", "phone");
        assertThat(log.getRequestSnapshotRef()).isEqualTo("snap:req:1");
        assertThat(log.getResponseSnapshotRef()).isEqualTo("snap:res:1");
    }
}
