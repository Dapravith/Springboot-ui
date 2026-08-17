package com.springboot.common.domain;

/**
 * Base type for every deliberate, expected domain failure.
 *
 * <p>The {@code code} is a stable, machine-readable identifier that survives
 * message rewording, so clients and dashboards can key off it. The web layer
 * maps subclasses of this exception to HTTP statuses in one place; anything not
 * derived from it is treated as an unexpected fault and reported as a 500.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
