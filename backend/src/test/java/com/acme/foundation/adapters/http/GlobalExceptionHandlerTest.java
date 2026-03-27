package com.acme.foundation.adapters.http;

import com.acme.foundation.adapters.inbound.http.error.GlobalExceptionHandler;
import com.acme.foundation.adapters.inbound.http.model.ProblemDetailResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    static final class ValidationProbe {
        @SuppressWarnings("unused")
        void submit(String name) {
        }
    }

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    void handleValidation_returns422WithRfc9457() throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "name", "must not be blank"));
        MethodParameter parameter = new MethodParameter(
            ValidationProbe.class.getDeclaredMethod("submit", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ProblemDetailResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().type()).contains("validation-error");
        assertThat(response.getBody().title()).isEqualTo("Validation Error");
    }

    @Test
    void handleMethodNotAllowed_returns405WithRfc9457() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ProblemDetailResponse> response = handler.handleMethodNotAllowed(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(405);
        assertThat(response.getBody().type()).contains("method-not-allowed");
    }

    @Test
    void handleAuthenticationException_returns401WithRfc9457() {
        BadCredentialsException ex = new BadCredentialsException("bad token");

        ResponseEntity<ProblemDetailResponse> response = handler.handleAuthenticationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().type()).contains("unauthorized");
    }

    @Test
    void handleAccessDeniedException_returns403WithRfc9457() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");

        ResponseEntity<ProblemDetailResponse> response = handler.handleAccessDeniedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().type()).contains("forbidden");
    }

    @Test
    void handleUnexpected_returns500WithRfc9457_noStackTrace() {
        Exception ex = new RuntimeException("something went wrong");

        ResponseEntity<ProblemDetailResponse> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        // Must not expose internal error details in the response
        assertThat(response.getBody().detail()).doesNotContain("something went wrong");
        assertThat(response.getBody().detail()).doesNotContain("RuntimeException");
    }

    @Test
    void problemDetail_instanceMatchesRequestUri() {
        BadCredentialsException ex = new BadCredentialsException("bad token");

        ResponseEntity<ProblemDetailResponse> response = handler.handleAuthenticationException(ex, request);

        assertThat(response.getBody().instance()).isEqualTo("/api/v1/test");
    }
}
