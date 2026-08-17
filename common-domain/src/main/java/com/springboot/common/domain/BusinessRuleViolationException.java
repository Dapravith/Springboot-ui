package com.springboot.common.domain;

/**
 * A request was well-formed but violates a business invariant.
 * Maps to HTTP 422 - the syntax was fine, the meaning was not.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
