package com.acme.foundation.adapters.http;

import com.acme.foundation.adapters.inbound.http.model.ApiResponse;
import com.acme.foundation.adapters.inbound.http.model.ResponseMeta;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ofData_setsDataAndNullMeta() {
        ApiResponse<String> response = ApiResponse.of("hello");

        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.meta()).isNull();
    }

    @Test
    void ofDataWithMeta_setsBothFields() {
        ResponseMeta meta = ResponseMeta.defaults();
        ApiResponse<Map<String, Object>> response = ApiResponse.of(Map.of("k", "v"), meta);

        assertThat(response.data()).containsEntry("k", "v");
        assertThat(response.meta()).isEqualTo(meta);
        assertThat(response.meta().apiVersion()).isEqualTo("v1");
        assertThat(response.meta().timestamp()).isNotNull();
    }

    @Test
    void dataIsRequired_canBeAnyObject() {
        ApiResponse<Integer> response = ApiResponse.of(42);
        assertThat(response.data()).isEqualTo(42);
    }
}
