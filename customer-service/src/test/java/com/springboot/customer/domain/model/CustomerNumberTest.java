package com.springboot.customer.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerNumberTest {

    @Test
    void acceptsWellFormedNumber() {
        assertEquals("CUS0123456789", CustomerNumber.of("CUS0123456789").value());
    }

    @Test
    void rejectsMalformedNumbers() {
        assertThrows(IllegalArgumentException.class, () -> CustomerNumber.of("ACC0123456789"));
        assertThrows(IllegalArgumentException.class, () -> CustomerNumber.of("CUS123"));
        assertThrows(IllegalArgumentException.class, () -> CustomerNumber.of("CUS01234567890"));
        assertThrows(IllegalArgumentException.class, () -> CustomerNumber.of(""));
        assertThrows(NullPointerException.class, () -> CustomerNumber.of(null));
    }
}
