package com.springboot.account.domain.model;

import com.springboot.common.domain.BusinessRuleViolationException;

public class NonPositiveAmountException extends BusinessRuleViolationException {

    public NonPositiveAmountException(String amount) {
        super("NON_POSITIVE_AMOUNT", "Amount must be greater than zero, got %s".formatted(amount));
    }
}
