package com.springboot.account.interfaces.rest.dto;

import com.springboot.account.domain.model.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(String accountNumber, String customerNumber, BigDecimal balance, String currency,
                              String status, Instant openedAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.accountNumber().value(),
                account.customerNumber(),
                account.balance().amount(),
                account.balance().currency().getCurrencyCode(),
                account.status().name(),
                account.openedAt());
    }
}
