package com.springboot.account.domain.model;

import com.springboot.common.domain.BusinessRuleViolationException;

public class AccountCurrencyMismatchException extends BusinessRuleViolationException {

    public AccountCurrencyMismatchException(String accountNumber, String accountCurrency, String amountCurrency) {
        super("ACCOUNT_CURRENCY_MISMATCH",
                "Account %s is held in %s and cannot move %s".formatted(accountNumber, accountCurrency, amountCurrency));
    }
}
