package com.acme.foundation.logging;

import com.acme.foundation.adapters.inbound.http.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StructuredLoggingIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void requestAddsCorrelationIdHeader() throws Exception {
        String correlationId = "test-correlation-id-123";

        MvcResult result = mockMvc.perform(get("/api/v1/health")
                .header(CorrelationIdFilter.CORRELATION_HEADER, correlationId))
                .andExpect(status().isOk())
                .andReturn();

        // Verify the correlation ID is echoed back in the response header
        assertThat(result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_HEADER))
                .isEqualTo(correlationId);
    }

    @Test
    @WithMockUser
    void requestWithoutCorrelationId_generatesFreshId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andReturn();

        String responseCorrelationId =
                result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_HEADER);
        assertThat(responseCorrelationId).isNotNull().isNotBlank();
    }
}
