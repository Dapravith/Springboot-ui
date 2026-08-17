package com.springboot.customer.interfaces.rest.dto;

import com.springboot.customer.domain.model.Customer;

import java.time.Instant;

/**
 * HTTP-facing output. Built explicitly from the domain model so that adding a
 * domain field never silently widens the public API.
 */
public record CustomerResponse(String customerNumber, String fullName, String email, String status,
                               Instant registeredAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.customerNumber().value(),
                customer.fullName(),
                customer.email(),
                customer.status().name(),
                customer.registeredAt());
    }
}
