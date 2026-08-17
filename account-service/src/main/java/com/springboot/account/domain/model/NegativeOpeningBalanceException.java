package com.springboot.account.domain.model;

import com.springboot.common.domain.BusinessRuleViolationException;

public class NegativeOpeningBalanceException extends BusinessRuleViolationException {

    public NegativeOpeningBalanceException(String amount) {
        super("NEGATIVE_OPENING_BALANCE", "Opening balance must not be negative, got %s".formatted(amount));
    }
}
