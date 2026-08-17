package com.springboot.common.web;

import com.springboot.common.domain.BusinessRuleViolationException;
import com.springboot.common.domain.ConflictException;
import com.springboot.common.domain.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapsTo404WithStableCode() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("Customer", "CUS1"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertFalse(response.getBody().success());
        assertNotNull(response.getBody().traceId());
    }

    @Test
    void conflictMapsTo409() {
        ResponseEntity<ApiError> response =
                handler.handleConflict(new ConflictException("DUPLICATE_EMAIL", "already registered"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("DUPLICATE_EMAIL", response.getBody().code());
    }

    @Test
    void businessRuleMapsTo422() {
        ResponseEntity<ApiError> response =
                handler.handleBusinessRule(new BusinessRuleViolationException("LIMIT_EXCEEDED", "too much"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("LIMIT_EXCEEDED", response.getBody().code());
    }

    @Test
    void unexpectedExceptionDoesNotLeakInternalDetail() {
        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new IllegalStateException("jdbc://user:hunter2@db/secret"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertFalse(response.getBody().message().contains("hunter2"),
                "internal detail must never reach the client");
        assertTrue(response.getBody().traceId().length() > 0,
                "client needs a trace id to quote when reporting the failure");
    }

    @Test
    void frameworkStatusCodesAreMappedToStableCodes() {
        // Regression guard: Spring MVC's own exceptions (NoResourceFoundException,
        // HttpRequestMethodNotSupportedException) implement the ErrorResponse
        // interface but share no Throwable base class, so a plain
        // @ExceptionHandler(Exception.class) used to swallow them and report a 404
        // as a 500. Extending ResponseEntityExceptionHandler is what fixes that;
        // these assertions pin the resulting codes.
        assertEquals("NOT_FOUND", GlobalExceptionHandler.codeFor(HttpStatus.NOT_FOUND));
        assertEquals("METHOD_NOT_ALLOWED", GlobalExceptionHandler.codeFor(HttpStatus.METHOD_NOT_ALLOWED));
        assertEquals("UNSUPPORTED_MEDIA_TYPE", GlobalExceptionHandler.codeFor(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }
}
