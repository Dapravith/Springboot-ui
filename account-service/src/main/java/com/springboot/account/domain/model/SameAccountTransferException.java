package com.springboot.account.domain.model;

import com.springboot.common.domain.BusinessRuleViolationException;

public class SameAccountTransferException extends BusinessRuleViolationException {

    public SameAccountTransferException(String accountNumber) {
        super("SAME_ACCOUNT_TRANSFER", "Cannot transfer from account %s to itself".formatted(accountNumber));
    }
}
