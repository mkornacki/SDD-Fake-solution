package com.acme.reservation.domain;

import com.acme.reservation.domain.audit.SensitiveDataMasking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("T035: Sensitive data masking unit tests")
class SensitiveDataMaskingTest {

    @Test
    @DisplayName("Masks PII and credentials in operator-visible snapshots")
    void masksSensitiveFields() {
        Map<String, Object> input = Map.of(
                "givenName", "Jane",
                "familyName", "Doe",
                "email", "jane@example.com",
                "password", "super-secret",
                "token", "abcd-token",
                "traceId", "trace-1",
                "status", "QUEUED");

        Map<String, Object> masked = SensitiveDataMasking.maskSnapshot(input);

        assertThat(masked.get("givenName")).isEqualTo("***");
        assertThat(masked.get("familyName")).isEqualTo("***");
        assertThat(masked.get("email")).isEqualTo("***");
        assertThat(masked.get("password")).isEqualTo("***");
        assertThat(masked.get("token")).isEqualTo("***");
        assertThat(masked.get("status")).isEqualTo("QUEUED");
    }

    @Test
    @DisplayName("Serialized snapshot does not leak common sensitive values")
    void sanitizedSnapshot_doesNotLeakPii() {
        Map<String, Object> input = Map.of(
                "email", "jane@example.com",
                "apiKey", "key-123",
                "status", "PROCESSING");

        String snapshot = SensitiveDataMasking.toMaskedSnapshot(input);

        assertThat(snapshot).doesNotContain("jane@example.com");
        assertThat(snapshot).doesNotContain("key-123");
        assertThat(snapshot).contains("\"email\":\"***\"");
        assertThat(snapshot).contains("\"apiKey\":\"***\"");
    }
}