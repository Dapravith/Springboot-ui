package com.springboot.transfer.application;

import com.springboot.common.audit.AuditLogger;
import com.springboot.transfer.domain.model.Transfer;
import com.springboot.transfer.domain.model.TransferNotFoundException;
import com.springboot.transfer.domain.model.TransferPolicy;
import com.springboot.transfer.domain.model.TransferReference;
import com.springboot.transfer.domain.port.in.QueryTransferUseCase;
import com.springboot.transfer.domain.port.in.SubmitTransferUseCase;
import com.springboot.transfer.domain.port.out.LedgerPort;
import com.springboot.transfer.domain.port.out.TransferRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates transfer submission.
 *
 * <p>The sequence is deliberate and auditable: evaluate policy, record the
 * outcome, and only then attempt to post to the ledger. Every transfer that was
 * ever attempted therefore has a durable record, including the ones that later
 * failed downstream - which is what an operator needs when reconciling.
 */
@Service
public class TransferApplicationService implements SubmitTransferUseCase, QueryTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferApplicationService.class);

    private static final String RESOURCE = "Transfer";
    private static final String ACTION_SUBMIT = "TRANSFER_SUBMITTED";
    private static final String ACTION_POST = "TRANSFER_POSTED";

    private final TransferPolicy policy;
    private final TransferRepositoryPort repository;
    private final LedgerPort ledger;
    private final AuditLogger audit;

    public TransferApplicationService(TransferPolicy policy, TransferRepositoryPort repository, LedgerPort ledger,
                                      AuditLogger audit) {
        this.policy = policy;
        this.repository = repository;
        this.ledger = ledger;
        this.audit = audit;
    }

    @Override
    public Transfer submit(SubmitTransferCommand command) {
        TransferReference reference = TransferReference.generate();

        Transfer transfer = policy
                .refusalReason(command.fromAccountNumber(), command.toAccountNumber(), command.amount())
                .map(reason -> Transfer.rejected(reference, command.fromAccountNumber(),
                        command.toAccountNumber(), command.amount(), reason))
                .orElseGet(() -> Transfer.accepted(reference, command.fromAccountNumber(),
                        command.toAccountNumber(), command.amount()));

        repository.save(transfer);

        Map<String, String> context = Map.of(
                "from", command.fromAccountNumber(),
                "to", command.toAccountNumber(),
                "amount", command.amount().amount().toPlainString(),
                "currency", command.amount().currency().getCurrencyCode());

        if (!transfer.isAccepted()) {
            audit.failure(ACTION_SUBMIT, RESOURCE, reference.value(), transfer.reason(), context);
            return transfer;
        }

        audit.success(ACTION_SUBMIT, RESOURCE, reference.value(), context);

        try {
            ledger.postTransfer(command.fromAccountNumber(), command.toAccountNumber(), command.amount());
            transfer.markPosted();
            audit.success(ACTION_POST, RESOURCE, reference.value(), context);
        } catch (LedgerPort.LedgerException ex) {
            log.warn("Ledger refused transfer {}: {}", reference, ex.getMessage());
            transfer.markFailed(ex.getMessage());
            audit.failure(ACTION_POST, RESOURCE, reference.value(), ex.getMessage(), context);
        }

        return repository.save(transfer);
    }

    @Override
    public List<Transfer> findAll() {
        return repository.findAll();
    }

    @Override
    public Transfer getByReference(TransferReference reference) {
        return repository.findByReference(reference)
                .orElseThrow(() -> new TransferNotFoundException(reference.value()));
    }
}
