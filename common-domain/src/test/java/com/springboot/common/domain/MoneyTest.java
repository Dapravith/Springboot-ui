package com.springboot.common.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void scalesToCurrencyMinorUnits() {
        assertEquals("10.00 USD", Money.of("10", "USD").toString());
        assertEquals("10 JPY", Money.of("10", "JPY").toString(), "JPY has no minor units");
    }

    @Test
    void addsAndSubtractsWithinOneCurrency() {
        Money balance = Money.of("100.00", "USD");

        assertEquals(Money.of("125.50", "USD"), balance.plus(Money.of("25.50", "USD")));
        assertEquals(Money.of("74.50", "USD"), balance.minus(Money.of("25.50", "USD")));
    }

    @Test
    void refusesArithmeticAcrossCurrencies() {
        Money usd = Money.of("100.00", "USD");
        Money eur = Money.of("100.00", "EUR");

        assertThrows(CurrencyMismatchException.class, () -> usd.plus(eur));
        assertThrows(CurrencyMismatchException.class, () -> usd.minus(eur));
        assertThrows(CurrencyMismatchException.class, () -> usd.isGreaterThan(eur));
    }

    @Test
    void comparesAmounts() {
        Money ten = Money.of("10.00", "USD");
        Money twenty = Money.of("20.00", "USD");

        assertTrue(twenty.isGreaterThan(ten));
        assertTrue(ten.isLessThan(twenty));
        assertFalse(ten.isGreaterThan(ten), "comparison is strict");
    }

    @Test
    void reportsSign() {
        assertTrue(Money.of("0.01", "USD").isPositive());
        assertTrue(Money.of("-0.01", "USD").isNegative());
        assertFalse(Money.zero("USD").isPositive());
        assertFalse(Money.zero("USD").isNegative());
    }

    @Test
    void equalityIgnoresTrailingZeroDifferences() {
        assertEquals(Money.of("10", "USD"), Money.of("10.00", "USD"),
                "both normalise to the same scale");
    }

    @Test
    void rejectsAmountFinerThanCurrencyPrecision() {
        assertThrows(ArithmeticException.class, () -> Money.of(new BigDecimal("10.001"), "USD"));
    }
}
