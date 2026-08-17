package com.springboot.customer.infrastructure.identity;

import com.springboot.customer.domain.model.CustomerNumber;
import com.springboot.customer.domain.port.out.CustomerNumberGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Allocates customer numbers from a cryptographically strong source, so numbers
 * are not guessable from one another. A uniqueness constraint on the column is
 * the authoritative guard against the rare collision.
 */
@Component
class RandomCustomerNumberGenerator implements CustomerNumberGeneratorPort {

    private static final int DIGITS = 10;

    private final SecureRandom random = new SecureRandom();

    @Override
    public CustomerNumber next() {
        StringBuilder sb = new StringBuilder("CUS");
        for (int i = 0; i < DIGITS; i++) {
            sb.append(random.nextInt(10));
        }
        return CustomerNumber.of(sb.toString());
    }
}
