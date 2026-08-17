package com.springboot.account.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** The externally visible identifier for an account. */
public record AccountNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("ACC\\d{12}");

    public AccountNumber {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Account number must match ACC followed by 12 digits, got: " + value);
        }
    }

    public static AccountNumber of(String value) {
        return new AccountNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
