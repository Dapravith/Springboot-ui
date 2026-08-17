package com.springboot.customer.infrastructure.persistence;

import com.springboot.customer.domain.model.Customer;
import com.springboot.customer.domain.model.CustomerNumber;
import com.springboot.customer.domain.model.CustomerStatus;

/**
 * Translates between the domain model and the persistence entity.
 *
 * <p>The single point where the two representations meet, so a schema change
 * shows up as a compile error here rather than leaking through the codebase.
 */
final class CustomerJpaMapper {

    private CustomerJpaMapper() {
    }

    static CustomerJpaEntity toEntity(Customer customer) {
        return new CustomerJpaEntity(
                customer.customerNumber().value(),
                customer.fullName(),
                customer.email(),
                customer.status().name(),
                customer.registeredAt());
    }

    static Customer toDomain(CustomerJpaEntity entity) {
        return Customer.rehydrate(
                CustomerNumber.of(entity.getCustomerNumber()),
                entity.getFullName(),
                entity.getEmail(),
                entity.getRegisteredAt(),
                CustomerStatus.valueOf(entity.getStatus()));
    }
}
