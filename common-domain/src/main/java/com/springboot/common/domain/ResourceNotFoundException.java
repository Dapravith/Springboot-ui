package com.springboot.common.domain;

/** The requested aggregate does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, String identifier) {
        super("RESOURCE_NOT_FOUND", "%s %s was not found".formatted(resource, identifier));
    }
}
