package com.acme.foundation.adapters.inbound.http.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Optional metadata added to API response envelopes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ResponseMeta {

    private final Instant timestamp;
    private final String apiVersion;

    public ResponseMeta(Instant timestamp, String apiVersion) {
        this.timestamp = timestamp;
        this.apiVersion = apiVersion;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String apiVersion() {
        return apiVersion;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public static ResponseMeta defaults() {
        return new ResponseMeta(Instant.now(), "v1");
    }
}
