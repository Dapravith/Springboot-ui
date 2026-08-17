package com.springboot.customer.domain.model;

import com.springboot.common.domain.ConflictException;

public class DuplicateEmailException extends ConflictException {

    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "Email %s is already registered".formatted(email));
    }
}
