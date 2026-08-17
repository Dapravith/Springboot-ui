package com.springboot.customer.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The externally visible identifier for a customer.
 *
 * <p>A value object rather than a bare String, so that a customer number cannot
 * be passed where an account number is expected, and so the format is validated
 * in exactly one place.
 */
public record CustomerNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("CUS\\d{10}");

    public CustomerNumber {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Customer number must match CUS followed by 10 digits, got: " + value);
        }
    }

    public static CustomerNumber of(String value) {
        return new CustomerNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
