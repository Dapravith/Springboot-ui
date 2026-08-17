package com.springboot.customer.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Customer aggregate root.
 *
 * <p>Deliberately free of JPA, Jackson and Spring annotations. Persistence is
 * handled by a separate entity in the infrastructure layer, which means the
 * database schema can change shape without the domain model following it, and
 * the domain can be unit-tested with no container at all.
 */
public final class Customer {

    private final CustomerNumber customerNumber;
    private final String fullName;
    private final String email;
    private final Instant registeredAt;
    private CustomerStatus status;

    private Customer(CustomerNumber customerNumber, String fullName, String email, Instant registeredAt,
                     CustomerStatus status) {
        this.customerNumber = Objects.requireNonNull(customerNumber, "customerNumber");
        this.fullName = requireText(fullName, "fullName");
        this.email = requireText(email, "email");
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt");
        this.status = Objects.requireNonNull(status, "status");
    }

    /** Creates a newly registered customer. */
    public static Customer register(CustomerNumber customerNumber, String fullName, String email) {
        return new Customer(customerNumber, fullName, email, Instant.now(), CustomerStatus.ACTIVE);
    }

    /** Rebuilds a customer from storage without re-running registration rules. */
    public static Customer rehydrate(CustomerNumber customerNumber, String fullName, String email,
                                     Instant registeredAt, CustomerStatus status) {
        return new Customer(customerNumber, fullName, email, registeredAt, status);
    }

    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public CustomerNumber customerNumber() {
        return customerNumber;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public CustomerStatus status() {
        return status;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Customer c && customerNumber.equals(c.customerNumber);
    }

    @Override
    public int hashCode() {
        return customerNumber.hashCode();
    }
}
