package com.acme.foundation.adapters.inbound.http.error;

import com.acme.foundation.adapters.inbound.http.model.ProblemDetailResponse;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import javax.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global RFC 9457 exception handler for all controller-layer errors.
 * Returns application/problem+json for every error scenario.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String BASE_TYPE = "https://api.acme.com/problems/";
    static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    /* ---- Validation errors ---- */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "validation-error")
                .title("Validation Error")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("One or more fields failed validation.")
                .instance(request.getRequestURI())
                .extensions(Map.of("errors", errors))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetailResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<String> errors = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "validation-error")
                .title("Validation Error")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Constraint violation detected.")
                .instance(request.getRequestURI())
                .extensions(Map.of("errors", errors))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    /* ---- HTTP protocol errors ---- */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetailResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "method-not-allowed")
                .title("Method Not Allowed")
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .detail("HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetailResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "bad-request")
                .title("Bad Request")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Request body is missing or cannot be read.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetailResponse> handleResourceNotFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "not-found")
                .title("Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail("The requested resource was not found.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetailResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "not-found")
                .title("Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage() != null ? ex.getMessage() : "Resource not found.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    /* ---- Security errors ---- */

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetailResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "unauthorized")
                .title("Unauthorized")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail("Authentication is required to access this resource.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetailResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "forbidden")
                .title("Forbidden")
                .status(HttpStatus.FORBIDDEN.value())
                .detail("You do not have permission to perform this operation.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        if (ex.getMessage() != null && ex.getMessage().contains("conflicting business context")) {
            ProblemDetailResponse body = ProblemDetailResponse.builder()
                    .type(BASE_TYPE + "conflict")
                    .title("Conflict")
                    .status(HttpStatus.CONFLICT.value())
                    .detail(ex.getMessage())
                    .instance(request.getRequestURI())
                    .build();

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .contentType(PROBLEM_JSON)
                    .body(body);
        }

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "bad-request")
                .title("Bad Request")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage() != null ? ex.getMessage() : "Invalid request.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(UnexpectedRollbackException.class)
    public ResponseEntity<ProblemDetailResponse> handleUnexpectedRollback(
            UnexpectedRollbackException ex, HttpServletRequest request) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "conflict")
                .title("Conflict")
                .status(HttpStatus.CONFLICT.value())
                .detail("Concurrent request conflict detected. Please retry with the same idempotency key.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "startup-validation")
                .title("Startup Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage() != null ? ex.getMessage() : "Invalid startup configuration.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(body);
    }

    /* ---- Catch-all ---- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        LOG.error("Unexpected error processing request {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .type(BASE_TYPE + "internal-server-error")
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred. Please try again later.")
                .instance(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(PROBLEM_JSON)
                .body(body);
    }
}
