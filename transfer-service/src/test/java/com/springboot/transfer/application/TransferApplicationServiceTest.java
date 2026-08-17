package com.springboot.transfer.application;

import com.springboot.common.audit.RecordingAuditLogger;
import com.springboot.common.domain.Money;
import com.springboot.transfer.domain.model.Transfer;
import com.springboot.transfer.domain.model.TransferNotFoundException;
import com.springboot.transfer.domain.model.TransferPolicy;
import com.springboot.transfer.domain.model.TransferReference;
import com.springboot.transfer.domain.model.TransferStatus;
import com.springboot.transfer.domain.port.in.SubmitTransferUseCase.SubmitTransferCommand;
import com.springboot.transfer.domain.port.out.LedgerPort;
import com.springboot.transfer.domain.port.out.TransferRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the orchestration with hand-written fakes for both outbound ports.
 *
 * <p>No Spring context, no HTTP, no database - which is the practical payoff of
 * declaring the ports in the domain: the interesting logic runs in milliseconds
 * and every downstream failure is trivial to simulate.
 */
class TransferApplicationServiceTest {

    private final FakeRepository repository = new FakeRepository();
    private final RecordingAuditLogger audit = new RecordingAuditLogger();

    private TransferApplicationService serviceWith(LedgerPort ledger) {
        return new TransferApplicationService(
                new TransferPolicy(Money.of("10000.00", "USD")), repository, ledger, audit);
    }

    private SubmitTransferCommand command(String amount) {
        return new SubmitTransferCommand("ACC000000000001", "ACC000000000002", Money.of(amount, "USD"));
    }

    @Test
    void postsAnAcceptedTransferToTheLedger() {
        RecordingLedger ledger = new RecordingLedger();

        Transfer transfer = serviceWith(ledger).submit(command("250.00"));

        assertEquals(TransferStatus.POSTED, transfer.status());
        assertEquals(1, ledger.calls.size());
        assertEquals(Money.of("250.00", "USD"), ledger.calls.getFirst());
    }

    @Test
    void rejectedTransferNeverReachesTheLedger() {
        RecordingLedger ledger = new RecordingLedger();

        Transfer transfer = serviceWith(ledger).submit(command("10000.01"));

        assertEquals(TransferStatus.REJECTED, transfer.status());
        assertTrue(ledger.calls.isEmpty(), "policy must refuse before any money is moved");
        assertTrue(transfer.reason().contains("single-transfer limit"));
    }

    @Test
    void ledgerFailureIsRecordedRatherThanThrown() {
        TransferApplicationService service = serviceWith((from, to, amount) -> {
            throw new LedgerPort.LedgerException("Ledger is unreachable");
        });

        Transfer transfer = service.submit(command("100.00"));

        assertEquals(TransferStatus.FAILED, transfer.status());
        assertEquals("Ledger is unreachable", transfer.reason());
    }

    @Test
    void everyAttemptIsPersistedIncludingFailures() {
        serviceWith((from, to, amount) -> {
            throw new LedgerPort.LedgerException("boom");
        }).submit(command("100.00"));

        assertEquals(1, repository.findAll().size(),
                "a failed transfer must still leave an audit record");
        assertEquals(TransferStatus.FAILED, repository.findAll().getFirst().status());
    }

    @Test
    void postedTransferIsAuditedTwice() {
        serviceWith(new RecordingLedger()).submit(command("250.00"));

        assertEquals(1, audit.withAction("TRANSFER_SUBMITTED").size());
        assertEquals(1, audit.withAction("TRANSFER_POSTED").size());
        assertTrue(audit.failures().isEmpty());
    }

    @Test
    void ledgerFailureIsAudited() {
        serviceWith((from, to, amount) -> {
            throw new LedgerPort.LedgerException("Ledger is unreachable");
        }).submit(command("100.00"));

        assertEquals(1, audit.withAction("TRANSFER_SUBMITTED").size(), "submission still succeeded");
        assertEquals(1, audit.failures().size());
        assertEquals("Ledger is unreachable", audit.failures().getFirst().reason());
    }

    @Test
    void rejectedTransferIsAuditedAsFailureAndNeverPosted() {
        serviceWith(new RecordingLedger()).submit(command("10000.01"));

        assertEquals(1, audit.failures().size());
        assertTrue(audit.withAction("TRANSFER_POSTED").isEmpty());
    }

    @Test
    void unknownReferenceIsReportedAsNotFound() {
        TransferApplicationService service = serviceWith(new RecordingLedger());

        assertThrows(TransferNotFoundException.class,
                () -> service.getByReference(TransferReference.of("does-not-exist")));
    }

    private static final class RecordingLedger implements LedgerPort {
        private final List<Money> calls = new ArrayList<>();

        @Override
        public void postTransfer(String fromAccountNumber, String toAccountNumber, Money amount) {
            calls.add(amount);
        }
    }

    private static final class FakeRepository implements TransferRepositoryPort {
        private final Map<String, Transfer> store = new HashMap<>();

        @Override
        public Transfer save(Transfer transfer) {
            store.put(transfer.reference().value(), transfer);
            return transfer;
        }

        @Override
        public Optional<Transfer> findByReference(TransferReference reference) {
            return Optional.ofNullable(store.get(reference.value()));
        }

        @Override
        public List<Transfer> findAll() {
            return List.copyOf(store.values());
        }
    }
}
