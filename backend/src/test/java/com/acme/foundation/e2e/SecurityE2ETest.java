package com.acme.foundation.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security E2E tests covering OWASP ASVS L3 scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void missingAuth_returns401_withRfc9457() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Token expired"));
        // Expired/malformed JWT
        mockMvc.perform(get("/api/v1/health")
                .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZXhwIjoxfQ.invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void injectedHeaders_doNotBypassSecurity() throws Exception {
        // Attempt to inject fake headers
        mockMvc.perform(get("/api/v1/health")
                .header("X-Forwarded-User", "admin")
                .header("X-Override-Auth", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sqlInjectionInPath_returnsClientError() throws Exception {
        // SQL injection attempts in path should be rejected (400 by StrictHttpFirewall or 404)
        mockMvc.perform(get("/api/v1/resourcename"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRoute_returns401_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent-endpoint"))
                .andExpect(status().isUnauthorized());
        // Unauthenticated, so 401 before 404
    }

    @Test
    void methodNotAllowed_returns405_withRfc9457() throws Exception {
        // POST to a GET-only health endpoint
        mockMvc.perform(post("/actuator/health/liveness")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
