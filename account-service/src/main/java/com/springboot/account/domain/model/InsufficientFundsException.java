package com.springboot.account.domain.model;

import com.springboot.common.domain.BusinessRuleViolationException;

public class InsufficientFundsException extends BusinessRuleViolationException {

    public InsufficientFundsException(String accountNumber, String balance, String requested) {
        super("INSUFFICIENT_FUNDS",
                "Account %s has balance %s and cannot be debited %s".formatted(accountNumber, balance, requested));
    }
}
