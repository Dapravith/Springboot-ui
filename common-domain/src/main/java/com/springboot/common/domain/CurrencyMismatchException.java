package com.springboot.common.domain;

/** Arithmetic or comparison was attempted between two different currencies. */
public class CurrencyMismatchException extends BusinessRuleViolationException {

    public CurrencyMismatchException(String expected, String actual) {
        super("CURRENCY_MISMATCH", "Expected currency %s but got %s".formatted(expected, actual));
    }
}
