package com.springboot.transfer.domain.port.in;

import com.springboot.common.domain.Money;
import com.springboot.transfer.domain.model.Transfer;

/** Inbound port: submitting a transfer for policy evaluation and posting. */
public interface SubmitTransferUseCase {

    Transfer submit(SubmitTransferCommand command);

    record SubmitTransferCommand(String fromAccountNumber, String toAccountNumber, Money amount) {

        public SubmitTransferCommand {
            if (fromAccountNumber == null || fromAccountNumber.isBlank()) {
                throw new IllegalArgumentException("fromAccountNumber must not be blank");
            }
            if (toAccountNumber == null || toAccountNumber.isBlank()) {
                throw new IllegalArgumentException("toAccountNumber must not be blank");
            }
            if (amount == null) {
                throw new IllegalArgumentException("amount must not be null");
            }
        }
    }
}
