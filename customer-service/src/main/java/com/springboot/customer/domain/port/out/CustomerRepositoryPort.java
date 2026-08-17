package com.springboot.customer.domain.port.out;

import com.springboot.customer.domain.model.Customer;
import com.springboot.customer.domain.model.CustomerNumber;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for customer persistence.
 *
 * <p>Declared in the domain and implemented in infrastructure: this is the
 * dependency inversion that lets the domain stay ignorant of JPA. Swapping
 * PostgreSQL for anything else is an infrastructure change only.
 */
public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findByCustomerNumber(CustomerNumber customerNumber);

    List<Customer> findAll();

    boolean existsByEmail(String email);
}
