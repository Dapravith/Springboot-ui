package com.springboot.customer.domain.model;

import com.springboot.common.domain.ResourceNotFoundException;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(String customerNumber) {
        super("Customer", customerNumber);
    }
}
