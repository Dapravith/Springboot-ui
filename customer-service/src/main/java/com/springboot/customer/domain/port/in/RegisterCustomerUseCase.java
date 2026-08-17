package com.springboot.customer.domain.port.in;

import com.springboot.customer.domain.model.Customer;

/**
 * Inbound port: registering a new customer.
 *
 * <p>The REST layer depends on this interface, never on the implementation, so
 * a second driving adapter (a Kafka consumer, a scheduled job, a CLI) can reuse
 * the identical use case without touching the application layer.
 */
public interface RegisterCustomerUseCase {

    Customer register(RegisterCustomerCommand command);

    /**
     * Input to the use case, expressed in domain terms rather than HTTP terms.
     * Validated at construction so an invalid command cannot exist.
     */
    record RegisterCustomerCommand(String fullName, String email) {

        public RegisterCustomerCommand {
            if (fullName == null || fullName.isBlank()) {
                throw new IllegalArgumentException("fullName must not be blank");
            }
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("email must not be blank");
            }
            fullName = fullName.trim();
            email = email.trim().toLowerCase();
        }
    }
}
