package com.springboot.account.domain.port.in;

import com.springboot.account.domain.model.AccountNumber;
import com.springboot.common.domain.Money;

/**
 * Inbound port: moving money between two accounts held by this service.
 *
 * <p>This is the authoritative balance movement. transfer-service decides
 * whether a transfer is permitted by policy; this use case performs it
 * atomically or not at all.
 */
public interface TransferFundsUseCase {

    void transfer(TransferCommand command);

    record TransferCommand(AccountNumber from, AccountNumber to, Money amount) {

        public TransferCommand {
            if (from == null || to == null || amount == null) {
                throw new IllegalArgumentException("from, to and amount are required");
            }
        }
    }
}
