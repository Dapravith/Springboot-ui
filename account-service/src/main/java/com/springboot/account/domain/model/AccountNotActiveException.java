package com.springboot.account.domain.model;

import com.springboot.common.domain.BusinessRuleViolationException;

public class AccountNotActiveException extends BusinessRuleViolationException {

    public AccountNotActiveException(String accountNumber, String status) {
        super("ACCOUNT_NOT_ACTIVE", "Account %s is %s and cannot move money".formatted(accountNumber, status));
    }
}
