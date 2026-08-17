package com.springboot.customer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Persistence representation of a customer. Separate from the domain model on
 * purpose: the domain stays framework-free, and the table can be reshaped
 * without altering domain code.
 *
 * <p>Identifier strategy is SEQUENCE with a pooled allocation, not IDENTITY.
 * IDENTITY forces Hibernate to flush every insert immediately to read the
 * generated key, which disables JDBC batching entirely.
 */
@Entity
@Table(name = "customer")
public class CustomerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq_gen")
    @SequenceGenerator(name = "customer_seq_gen", sequenceName = "customer_seq", allocationSize = 50)
    private Long id;

    @Column(name = "customer_number", nullable = false, unique = true, length = 20)
    private String customerNumber;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    /** Stored as text; translated to and from the domain enum by the mapper. */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CustomerJpaEntity() {
        // for JPA
    }

    CustomerJpaEntity(String customerNumber, String fullName, String email, String status, Instant registeredAt) {
        this.customerNumber = customerNumber;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
        this.registeredAt = registeredAt;
    }

    Long getId() {
        return id;
    }

    String getCustomerNumber() {
        return customerNumber;
    }

    String getFullName() {
        return fullName;
    }

    String getEmail() {
        return email;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    Instant getRegisteredAt() {
        return registeredAt;
    }
}
