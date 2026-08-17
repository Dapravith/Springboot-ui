package com.springboot.account.infrastructure.identity;

import com.springboot.account.domain.model.AccountNumber;
import com.springboot.account.domain.port.out.AccountNumberGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Allocates account numbers from a cryptographically strong source so numbers
 * are not guessable. The unique constraint on the column is the authoritative
 * guard against the rare collision.
 */
@Component
class RandomAccountNumberGenerator implements AccountNumberGeneratorPort {

    private static final int DIGITS = 12;

    private final SecureRandom random = new SecureRandom();

    @Override
    public AccountNumber next() {
        StringBuilder sb = new StringBuilder("ACC");
        for (int i = 0; i < DIGITS; i++) {
            sb.append(random.nextInt(10));
        }
        return AccountNumber.of(sb.toString());
    }
}
