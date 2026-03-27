package com.acme.reservation.domain.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Utility for masking sensitive fields before persistence or operational export.
 */
public final class SensitiveDataMasking {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "givenname",
            "familyname",
            "email",
            "phone",
            "password",
            "secret",
            "token",
            "apikey",
            "credential");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SensitiveDataMasking() {
    }

    public static Map<String, Object> maskSnapshot(Map<String, Object> snapshot) {
        Map<String, Object> masked = new LinkedHashMap<>();
        snapshot.forEach((key, value) -> {
            if (isSensitiveKey(key)) {
                masked.put(key, "***");
            } else {
                masked.put(key, value);
            }
        });
        return masked;
    }

    public static String toMaskedSnapshot(Map<String, Object> snapshot) {
        try {
            return OBJECT_MAPPER.writeValueAsString(maskSnapshot(snapshot));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize masked snapshot", e);
        }
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT).replace("_", "");
        if (SENSITIVE_KEYS.contains(normalized)) {
            return true;
        }
        return normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("credential");
    }
}