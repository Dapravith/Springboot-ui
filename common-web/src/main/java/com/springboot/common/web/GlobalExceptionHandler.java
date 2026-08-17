package com.springboot.common.web;

import com.springboot.common.domain.BusinessRuleViolationException;
import com.springboot.common.domain.ConflictException;
import com.springboot.common.domain.DomainException;
import com.springboot.common.domain.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Single place where exceptions become HTTP responses.
 *
 * <p>Imported explicitly by each service rather than component-scanned, because
 * services scan only their own package. That keeps the wiring visible: a reader
 * of a service's configuration can see exactly where its error behaviour comes
 * from.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so that Spring MVC's own
 * exceptions keep their correct status codes. This is not optional polish: those
 * exceptions ({@code NoResourceFoundException},
 * {@code HttpRequestMethodNotSupportedException} and friends) implement the
 * {@code ErrorResponse} interface but do not extend a common {@code Throwable}
 * base class, so a plain {@code @ExceptionHandler(Exception.class)} swallows them
 * and reports a 404 as a 500 - while logging every stray probe at ERROR. The base
 * class enumerates them properly; {@link #handleExceptionInternal} then re-renders
 * them in this system's envelope.
 *
 * <p>Domain errors are mapped from the exception hierarchy, so adding a new domain
 * error does not require touching this class - only choosing the right base type.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Domain errors -----------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return respond(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleViolationException ex) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    /** Catch-all for any domain exception added later without a dedicated handler. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex);
    }

    // --- Validation on query/path parameters -------------------------------

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(ConstraintViolationException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.computeIfAbsent(violation.getPropertyPath().toString(), key -> new ArrayList<>())
                    .add(violation.getMessage());
        }
        return ResponseEntity.badRequest().body(validationBody(fieldErrors));
    }

    // --- Spring MVC's own exceptions ---------------------------------------

    /** Request body failed bean validation: report which fields and why. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.computeIfAbsent(error.getField(), key -> new ArrayList<>())
                        .add(error.getDefaultMessage()));

        return ResponseEntity.badRequest().headers(headers).body(validationBody(fieldErrors));
    }

    /** Malformed or unparseable request body. The caller's fault, so 400 rather than 500. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String traceId = newTraceId();
        log.debug("Unreadable request body [traceId={}]: {}", traceId, ex.getMessage());
        return ResponseEntity.badRequest().headers(headers)
                .body(ApiError.of("MALFORMED_REQUEST", "Request body could not be parsed", traceId));
    }

    /**
     * Every remaining framework exception - unknown path (404), wrong method (405),
     * unsupported media type (415) and the rest.
     *
     * <p>The status the framework chose is preserved rather than flattened, and the
     * event is logged at debug: a request for a path that does not exist is routine
     * traffic, not a server fault.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        String traceId = newTraceId();
        log.debug("Framework web exception [traceId={}, status={}]: {}",
                traceId, statusCode.value(), ex.getMessage());

        return ResponseEntity.status(statusCode).headers(headers)
                .body(ApiError.of(codeFor(statusCode), detailFor(body, statusCode), traceId));
    }

    // --- Last resort -------------------------------------------------------

    /**
     * Anything that is neither a domain error nor a known framework error.
     *
     * <p>The exception is logged in full server-side; the client gets only the
     * trace id, so internal details never leak in a response body.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        String traceId = newTraceId();
        log.error("Unhandled exception [traceId={}]", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred", traceId));
    }

    // --- Helpers -----------------------------------------------------------

    static String codeFor(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return (status != null) ? status.name() : "HTTP_" + statusCode.value();
    }

    private static String detailFor(@Nullable Object body, HttpStatusCode statusCode) {
        if (body instanceof ProblemDetail problem && problem.getDetail() != null) {
            return problem.getDetail();
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return (status != null) ? status.getReasonPhrase() : "Request could not be handled";
    }

    private ApiError validationBody(Map<String, List<String>> fieldErrors) {
        String traceId = newTraceId();
        log.debug("Request validation failed [traceId={}]: {}", traceId, fieldErrors);
        return ApiError.validation("Request validation failed", fieldErrors, traceId);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, DomainException ex) {
        String traceId = newTraceId();
        log.debug("Domain exception [traceId={}, code={}]: {}", traceId, ex.code(), ex.getMessage());
        return ResponseEntity.status(status).body(ApiError.of(ex.code(), ex.getMessage(), traceId));
    }

    private String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
