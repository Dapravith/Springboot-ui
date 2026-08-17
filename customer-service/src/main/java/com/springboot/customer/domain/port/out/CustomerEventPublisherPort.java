package com.springboot.customer.domain.port.out;

import com.springboot.customer.domain.model.Customer;

/**
 * Outbound port for publishing customer domain events.
 *
 * <p>Implementations must not propagate broker failures: event publication is
 * best-effort relative to the transaction that produced it, and a broker outage
 * must never fail customer registration.
 */
public interface CustomerEventPublisherPort {

    void customerRegistered(Customer customer);
}
