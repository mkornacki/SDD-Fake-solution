package com.acme.foundation.adapters.inbound.http.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard response envelope for successful API responses.
 *
 * @param <T> the type of the primary payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    private final T data;
    private final ResponseMeta meta;

    public ApiResponse(T data, ResponseMeta meta) {
        this.data = data;
        this.meta = meta;
    }

    public T data() {
        return data;
    }

    public T getData() {
        return data;
    }

    public ResponseMeta meta() {
        return meta;
    }

    public ResponseMeta getMeta() {
        return meta;
    }

    /**
     * Create a response with data only and no meta.
     */
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    /**
     * Create a response with data and meta information.
     */
    public static <T> ApiResponse<T> of(T data, ResponseMeta meta) {
        return new ApiResponse<>(data, meta);
    }
}
