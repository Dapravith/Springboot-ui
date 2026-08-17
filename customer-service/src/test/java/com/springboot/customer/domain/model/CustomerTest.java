package com.springboot.customer.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerTest {

    private static final CustomerNumber NUMBER = CustomerNumber.of("CUS0000000001");

    @Test
    void registersAsActive() {
        Customer customer = Customer.register(NUMBER, "Ada Lovelace", "ada@example.com");

        assertTrue(customer.isActive());
        assertEquals(CustomerStatus.ACTIVE, customer.status());
        assertNotNull(customer.registeredAt());
    }

    @Test
    void canBeDeactivated() {
        Customer customer = Customer.register(NUMBER, "Ada", "ada@example.com");

        customer.deactivate();

        assertFalse(customer.isActive());
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> Customer.register(NUMBER, "  ", "ada@example.com"));
        assertThrows(IllegalArgumentException.class, () -> Customer.register(NUMBER, "Ada", " "));
        assertThrows(NullPointerException.class, () -> Customer.register(null, "Ada", "ada@example.com"));
    }

    @Test
    void identityIsTheCustomerNumber() {
        Customer a = Customer.register(NUMBER, "Ada", "ada@example.com");
        Customer b = Customer.register(NUMBER, "Someone Else", "other@example.com");

        assertEquals(a, b, "same customer number means the same customer");
        assertEquals(a.hashCode(), b.hashCode());
    }
}
