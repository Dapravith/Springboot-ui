package com.springboot.transfer.interfaces.rest.dto;

import com.springboot.transfer.domain.model.Transfer;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(String reference, String fromAccountNumber, String toAccountNumber,
                               BigDecimal amount, String currency, String status, String reason,
                               Instant submittedAt) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.reference().value(),
                transfer.fromAccountNumber(),
                transfer.toAccountNumber(),
                transfer.amount().amount(),
                transfer.amount().currency().getCurrencyCode(),
                transfer.status().name(),
                transfer.reason(),
                transfer.submittedAt());
    }
}
