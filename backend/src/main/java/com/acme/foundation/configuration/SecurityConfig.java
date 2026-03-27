package com.acme.foundation.configuration;

import com.acme.foundation.adapters.inbound.http.model.ProblemDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

/**
 * Spring Security configuration — OAuth 2.0 / OIDC Resource Server.
 * All non-actuator endpoints require a valid JWT bearer token.
 * Authentication and authorization failures return RFC 9457 problem details.
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String PROBLEM_JSON_TYPE = "application/problem+json";
    private static final String BASE_TYPE = "https://api.acme.com/problems/";

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeRequests(auth -> auth
                    // Kubernetes health probes and API docs — unauthenticated
                    .antMatchers(
                            "/actuator/health/liveness",
                            "/actuator/health/readiness",
                            "/actuator/health/**",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).permitAll()
                    // DLQ admin operations — require admin:dlq scope (enforced via @PreAuthorize)
                    .antMatchers(org.springframework.http.HttpMethod.POST,
                            "/api/v1/operations/dlq/**").hasAuthority("SCOPE_admin:dlq")
                    // Write operations — require reservation:write scope
                    .antMatchers(org.springframework.http.HttpMethod.POST,
                            "/api/v1/reservations").hasAuthority("SCOPE_reservation:write")
                    .antMatchers(org.springframework.http.HttpMethod.DELETE,
                            "/api/v1/reservations/**").hasAuthority("SCOPE_reservation:write")
                    .antMatchers(org.springframework.http.HttpMethod.GET,
                            "/api/v1/dev/sample-data/status").hasAuthority("SCOPE_dev:sample-data")
                    // All other requests require authentication
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> {})
                    .authenticationEntryPoint(this::handleAuthFailure)
                    .accessDeniedHandler(this::handleAccessDenied)
            )
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(this::handleAuthFailure)
                    .accessDeniedHandler(this::handleAccessDenied)
            );

        return http.build();
    }

    private void handleAuthFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception)
            throws IOException {

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "unauthorized")
                .title("Unauthorized")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail("Bearer token is missing or invalid.")
                .instance(request.getRequestURI())
                .build();

        writeProblem(response, HttpStatus.UNAUTHORIZED.value(), problem);
    }

    private void handleAccessDenied(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException exception)
            throws IOException {

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "forbidden")
                .title("Forbidden")
                .status(HttpStatus.FORBIDDEN.value())
                .detail("Caller does not have permission to perform this operation.")
                .instance(request.getRequestURI())
                .build();

        writeProblem(response, HttpStatus.FORBIDDEN.value(), problem);
    }

    private void writeProblem(HttpServletResponse response, int status, ProblemDetailResponse problem)
            throws IOException {
        response.setStatus(status);
        response.setContentType(PROBLEM_JSON_TYPE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
