package com.springboot.transfer.domain.model;

import com.springboot.common.domain.Money;

import java.util.Objects;
import java.util.Optional;

/**
 * The rules that decide whether a transfer may be attempted.
 *
 * <p>A first-class domain object rather than a handful of ifs inside a service:
 * the policy is what this service exists to own, so it is testable on its own
 * and can gain rules (velocity limits, sanctions checks, cut-off times) without
 * touching orchestration code.
 */
public final class TransferPolicy {

    private final Money singleTransferLimit;

    public TransferPolicy(Money singleTransferLimit) {
        this.singleTransferLimit = Objects.requireNonNull(singleTransferLimit, "singleTransferLimit");
    }

    /**
     * @return the reason the transfer is refused, or empty when it is permitted
     */
    public Optional<String> refusalReason(String fromAccountNumber, String toAccountNumber, Money amount) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            return Optional.of("Source and destination accounts are identical");
        }
        if (!amount.isPositive()) {
            return Optional.of("Amount must be greater than zero");
        }
        if (!amount.isSameCurrency(singleTransferLimit)) {
            return Optional.of("Transfers are only supported in %s"
                    .formatted(singleTransferLimit.currency().getCurrencyCode()));
        }
        if (amount.isGreaterThan(singleTransferLimit)) {
            return Optional.of("Amount exceeds the single-transfer limit of " + singleTransferLimit);
        }
        return Optional.empty();
    }

    public Money singleTransferLimit() {
        return singleTransferLimit;
    }
}
