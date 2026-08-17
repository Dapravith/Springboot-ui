package com.springboot.common.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Uniform failure envelope.
 *
 * <p>{@code code} is the stable machine-readable identifier carried up from the
 * domain layer. {@code fieldErrors} is populated only for validation failures.
 * {@code traceId} lets a caller quote one value that ties their failed request
 * to the server-side logs and traces.
 */
public record ApiError(boolean success, String code, String message, Map<String, List<String>> fieldErrors,
                       String traceId, Instant timestamp) {

    public static ApiError of(String code, String message, String traceId) {
        return new ApiError(false, code, message, Map.of(), traceId, Instant.now());
    }

    public static ApiError validation(String message, Map<String, List<String>> fieldErrors, String traceId) {
        return new ApiError(false, "VALIDATION_FAILED", message, fieldErrors, traceId, Instant.now());
    }
}
