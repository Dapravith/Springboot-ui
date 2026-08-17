package com.springboot.customer.domain.port.in;

import com.springboot.customer.domain.model.Customer;
import com.springboot.customer.domain.model.CustomerNumber;

import java.util.List;

/**
 * Inbound port: read-side access to customers.
 *
 * <p>Kept separate from {@link RegisterCustomerUseCase} so that read and write
 * concerns can evolve, be secured, and be scaled independently.
 */
public interface QueryCustomerUseCase {

    List<Customer> findAll();

    /**
     * @throws com.springboot.customer.domain.model.CustomerNotFoundException if absent
     */
    Customer getByCustomerNumber(CustomerNumber customerNumber);
}
