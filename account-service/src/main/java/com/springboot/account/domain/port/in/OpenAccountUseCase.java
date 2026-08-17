package com.springboot.account.domain.port.in;

import com.springboot.account.domain.model.Account;
import com.springboot.common.domain.Money;

/** Inbound port: opening a new account. */
public interface OpenAccountUseCase {

    Account open(OpenAccountCommand command);

    record OpenAccountCommand(String customerNumber, Money openingBalance) {

        public OpenAccountCommand {
            if (customerNumber == null || customerNumber.isBlank()) {
                throw new IllegalArgumentException("customerNumber must not be blank");
            }
            if (openingBalance == null) {
                throw new IllegalArgumentException("openingBalance must not be null");
            }
        }
    }
}
