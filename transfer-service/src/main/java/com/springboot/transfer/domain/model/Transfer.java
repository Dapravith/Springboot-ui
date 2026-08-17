package com.springboot.transfer.domain.model;

import com.springboot.common.domain.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * A transfer request and its outcome.
 *
 * <p>State transitions are explicit and one-way: a transfer is first accepted
 * or rejected by policy, and an accepted transfer is then posted or failed by
 * the downstream ledger. Illegal transitions throw rather than silently
 * corrupting the audit trail.
 */
public final class Transfer {

    private final TransferReference reference;
    private final String fromAccountNumber;
    private final String toAccountNumber;
    private final Money amount;
    private final Instant submittedAt;
    private TransferStatus status;
    private String reason;

    private Transfer(TransferReference reference, String fromAccountNumber, String toAccountNumber, Money amount,
                     TransferStatus status, String reason, Instant submittedAt) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.fromAccountNumber = Objects.requireNonNull(fromAccountNumber, "fromAccountNumber");
        this.toAccountNumber = Objects.requireNonNull(toAccountNumber, "toAccountNumber");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.status = Objects.requireNonNull(status, "status");
        this.reason = reason;
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
    }

    public static Transfer accepted(TransferReference reference, String from, String to, Money amount) {
        return new Transfer(reference, from, to, amount, TransferStatus.ACCEPTED, null, Instant.now());
    }

    public static Transfer rejected(TransferReference reference, String from, String to, Money amount,
                                    String reason) {
        return new Transfer(reference, from, to, amount, TransferStatus.REJECTED, reason, Instant.now());
    }

    public void markPosted() {
        requireStatus(TransferStatus.ACCEPTED, "post");
        this.status = TransferStatus.POSTED;
    }

    public void markFailed(String reason) {
        requireStatus(TransferStatus.ACCEPTED, "fail");
        this.status = TransferStatus.FAILED;
        this.reason = reason;
    }

    public boolean isAccepted() {
        return status == TransferStatus.ACCEPTED;
    }

    private void requireStatus(TransferStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Cannot %s a transfer in status %s".formatted(action, status));
        }
    }

    public TransferReference reference() {
        return reference;
    }

    public String fromAccountNumber() {
        return fromAccountNumber;
    }

    public String toAccountNumber() {
        return toAccountNumber;
    }

    public Money amount() {
        return amount;
    }

    public TransferStatus status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    public Instant submittedAt() {
        return submittedAt;
    }
}
