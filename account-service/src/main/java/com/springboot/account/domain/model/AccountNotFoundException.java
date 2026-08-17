package com.springboot.account.domain.model;

import com.springboot.common.domain.ResourceNotFoundException;

public class AccountNotFoundException extends ResourceNotFoundException {

    public AccountNotFoundException(String accountNumber) {
        super("Account", accountNumber);
    }
}
