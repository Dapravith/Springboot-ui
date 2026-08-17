package com.springboot.transfer.domain.model;

import com.springboot.common.domain.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferTest {

    private Transfer accepted() {
        return Transfer.accepted(TransferReference.generate(), "ACC1", "ACC2", Money.of("10.00", "USD"));
    }

    @Test
    void acceptedTransferHasNoReason() {
        Transfer transfer = accepted();

        assertEquals(TransferStatus.ACCEPTED, transfer.status());
        assertTrue(transfer.isAccepted());
        assertNull(transfer.reason());
    }

    @Test
    void rejectedTransferCarriesReason() {
        Transfer transfer = Transfer.rejected(TransferReference.generate(), "ACC1", "ACC2",
                Money.of("10.00", "USD"), "over limit");

        assertEquals(TransferStatus.REJECTED, transfer.status());
        assertFalse(transfer.isAccepted());
        assertEquals("over limit", transfer.reason());
    }

    @Test
    void acceptedTransferCanBePosted() {
        Transfer transfer = accepted();

        transfer.markPosted();

        assertEquals(TransferStatus.POSTED, transfer.status());
    }

    @Test
    void acceptedTransferCanFail() {
        Transfer transfer = accepted();

        transfer.markFailed("ledger unreachable");

        assertEquals(TransferStatus.FAILED, transfer.status());
        assertEquals("ledger unreachable", transfer.reason());
    }

    @Test
    void rejectedTransferCannotBePosted() {
        Transfer transfer = Transfer.rejected(TransferReference.generate(), "ACC1", "ACC2",
                Money.of("10.00", "USD"), "over limit");

        assertThrows(IllegalStateException.class, transfer::markPosted);
    }

    @Test
    void postedTransferCannotBePostedTwice() {
        Transfer transfer = accepted();
        transfer.markPosted();

        assertThrows(IllegalStateException.class, transfer::markPosted,
                "double-posting would corrupt the audit trail");
    }
}
