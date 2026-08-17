package com.springboot.transfer.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Idempotency-friendly identifier for a submitted transfer. */
public record TransferReference(String value) {

    public TransferReference {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Transfer reference must not be blank");
        }
    }

    public static TransferReference generate() {
        return new TransferReference(UUID.randomUUID().toString());
    }

    public static TransferReference of(String value) {
        return new TransferReference(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
