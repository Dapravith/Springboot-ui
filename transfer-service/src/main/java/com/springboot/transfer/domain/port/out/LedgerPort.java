package com.springboot.transfer.domain.port.out;

import com.springboot.common.domain.Money;

/**
 * Outbound port for the authoritative ledger (account-service).
 *
 * <p>This service decides policy; the ledger owns balances. Keeping the ledger
 * behind a port means the transport - REST today, messaging or gRPC later - is
 * an infrastructure choice rather than a domain concern.
 */
public interface LedgerPort {

    /**
     * Posts the movement.
     *
     * @throws LedgerException when the ledger refuses or is unreachable
     */
    void postTransfer(String fromAccountNumber, String toAccountNumber, Money amount);

    /** Raised when the ledger refuses the movement or cannot be reached. */
    class LedgerException extends RuntimeException {
        public LedgerException(String message, Throwable cause) {
            super(message, cause);
        }

        public LedgerException(String message) {
            super(message);
        }
    }
}
