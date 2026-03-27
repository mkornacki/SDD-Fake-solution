package com.acme.reservation.domain;

import com.acme.reservation.domain.audit.RetentionPolicyRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("T034: RetentionPolicyRule unit tests")
class RetentionPolicyTest {

    @Test
    @DisplayName("RAW_PAYLOAD maps to DELETE disposition")
    void rawPayload_rule_deleteDisposition() {
        RetentionPolicyRule rule = RetentionPolicyRule.defaultRuleFor(
                RetentionPolicyRule.ArtifactClass.RAW_PAYLOAD);

        assertThat(rule.getDisposition()).isEqualTo(RetentionPolicyRule.Disposition.DELETE);
    }

    @Test
    @DisplayName("AUDIT_EVENT maps to ARCHIVE disposition")
    void auditEvent_rule_archiveDisposition() {
        RetentionPolicyRule rule = RetentionPolicyRule.defaultRuleFor(
                RetentionPolicyRule.ArtifactClass.AUDIT_EVENT);

        assertThat(rule.getDisposition()).isEqualTo(RetentionPolicyRule.Disposition.ARCHIVE);
    }
}