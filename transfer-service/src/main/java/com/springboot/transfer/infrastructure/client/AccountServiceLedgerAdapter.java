package com.springboot.transfer.infrastructure.client;

import com.springboot.common.domain.Money;
import com.springboot.transfer.domain.port.out.LedgerPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

/**
 * Talks to account-service, the authoritative ledger.
 *
 * <p>Every failure mode is folded into {@link LedgerException} so the
 * application layer never has to know that the ledger happens to be reached
 * over HTTP. A 4xx carries the ledger's own refusal message; anything else is
 * reported as unreachable.
 */
@Component
class AccountServiceLedgerAdapter implements LedgerPort {

    private final RestClient restClient;

    AccountServiceLedgerAdapter(RestClient accountServiceRestClient) {
        this.restClient = accountServiceRestClient;
    }

    record LedgerTransferRequest(String fromAccountNumber, String toAccountNumber, BigDecimal amount,
                                 String currency) {
    }

    @Override
    public void postTransfer(String fromAccountNumber, String toAccountNumber, Money amount) {
        LedgerTransferRequest body = new LedgerTransferRequest(
                fromAccountNumber, toAccountNumber, amount.amount(), amount.currency().getCurrencyCode());

        try {
            restClient.post()
                    .uri("/api/v1/accounts/transfers")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new LedgerException(
                    "Ledger refused the transfer (HTTP %d)".formatted(ex.getStatusCode().value()), ex);
        } catch (RestClientException ex) {
            throw new LedgerException("Ledger is unreachable", ex);
        }
    }
}
