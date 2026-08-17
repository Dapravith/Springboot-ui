package com.springboot.transfer.domain.model;

import com.springboot.common.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferPolicyTest {

    private final TransferPolicy policy = new TransferPolicy(Money.of("10000.00", "USD"));

    @Test
    void permitsTransferWithinLimit() {
        Optional<String> refusal = policy.refusalReason("ACC1", "ACC2", Money.of("250.00", "USD"));

        assertTrue(refusal.isEmpty());
    }

    @Test
    void limitBoundaryIsInclusive() {
        assertTrue(policy.refusalReason("ACC1", "ACC2", Money.of("10000.00", "USD")).isEmpty());
        assertTrue(policy.refusalReason("ACC1", "ACC2", Money.of("10000.01", "USD")).isPresent());
    }

    @Test
    void refusesTransferToSameAccount() {
        Optional<String> refusal = policy.refusalReason("ACC1", "ACC1", Money.of("10.00", "USD"));

        assertEquals("Source and destination accounts are identical", refusal.orElseThrow());
    }

    @Test
    void refusesNonPositiveAmount() {
        assertTrue(policy.refusalReason("ACC1", "ACC2", Money.zero("USD")).isPresent());
        assertTrue(policy.refusalReason("ACC1", "ACC2", Money.of("-1.00", "USD")).isPresent());
    }

    @Test
    void refusesUnsupportedCurrency() {
        Optional<String> refusal = policy.refusalReason("ACC1", "ACC2", Money.of("10.00", "EUR"));

        assertTrue(refusal.orElseThrow().contains("only supported in USD"),
                "a foreign-currency transfer must be refused, not compared against a USD limit");
    }
}
