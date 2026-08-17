package com.springboot.common.domain;

/** The request conflicts with existing state, e.g. a uniqueness clash. Maps to HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
