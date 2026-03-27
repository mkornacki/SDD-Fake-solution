package com.acme.foundation.e2e;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test for critical user flow: authenticated health check.
 * Full Playwright-based E2E tests require a running service and OIDC provider.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoundationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void authenticatedHealthFlow_returnsJsonEnvelope() throws Exception {
        // Simulate: obtain token (mocked) → call GET /api/v1/health → verify JSON envelope
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.meta.apiVersion").value("v1"));
    }

    @Test
    void healthProbes_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
