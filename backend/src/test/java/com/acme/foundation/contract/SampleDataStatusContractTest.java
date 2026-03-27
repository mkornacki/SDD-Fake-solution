package com.acme.foundation.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SampleDataStatusContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sampleDataStatus_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dev/sample-data/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_dev:sample-data")
    void sampleDataStatus_returnsExpectedShapeForAuthorizedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/dev/sample-data/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetName").exists())
                .andExpect(jsonPath("$.datasetVersion").exists())
                .andExpect(jsonPath("$.seedStatus").exists())
                .andExpect(jsonPath("$.recordCounts").isMap());
    }
}
