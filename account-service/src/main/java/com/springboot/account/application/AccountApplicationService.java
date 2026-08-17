package com.springboot.account.application;

import com.springboot.account.domain.model.Account;
import com.springboot.account.domain.model.AccountNotFoundException;
import com.springboot.account.domain.model.AccountNumber;
import com.springboot.account.domain.model.SameAccountTransferException;
import com.springboot.account.domain.port.in.OpenAccountUseCase;
import com.springboot.account.domain.port.in.QueryAccountUseCase;
import com.springboot.account.domain.port.in.TransferFundsUseCase;
import com.springboot.account.domain.port.out.AccountEventPublisherPort;
import com.springboot.account.domain.port.out.AccountMetricsPort;
import com.springboot.account.domain.port.out.AccountNumberGeneratorPort;
import com.springboot.account.domain.port.out.AccountRepositoryPort;
import com.springboot.common.audit.AuditLogger;
import com.springboot.common.domain.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the account use cases and owns the transaction boundaries.
 *
 * <p>The balance rules themselves live in {@link Account}; this class decides
 * only what happens inside which transaction, in what order, and what gets
 * written to the audit trail.
 */
@Service
public class AccountApplicationService implements OpenAccountUseCase, TransferFundsUseCase, QueryAccountUseCase {

    private static final String RESOURCE = "Account";
    private static final String ACTION_OPEN = "ACCOUNT_OPENED";
    private static final String ACTION_TRANSFER = "FUNDS_TRANSFERRED";

    private final AccountRepositoryPort repository;
    private final AccountNumberGeneratorPort numberGenerator;
    private final AccountEventPublisherPort eventPublisher;
    private final AccountMetricsPort metrics;
    private final AuditLogger audit;

    public AccountApplicationService(AccountRepositoryPort repository,
                                     AccountNumberGeneratorPort numberGenerator,
                                     AccountEventPublisherPort eventPublisher,
                                     AccountMetricsPort metrics,
                                     AuditLogger audit) {
        this.repository = repository;
        this.numberGenerator = numberGenerator;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.audit = audit;
    }

    @Override
    @Transactional
    public Account open(OpenAccountCommand command) {
        Account account = Account.open(numberGenerator.next(), command.customerNumber(), command.openingBalance());
        Account saved = repository.save(account);

        metrics.accountOpened();
        eventPublisher.accountOpened(saved);

        audit.success(ACTION_OPEN, RESOURCE, saved.accountNumber().value(), Map.of(
                "customerNumber", saved.customerNumber(),
                "openingBalance", saved.balance().amount().toPlainString(),
                "currency", saved.balance().currency().getCurrencyCode()));

        return saved;
    }

    /**
     * Moves money atomically between two accounts.
     *
     * <p>Both accounts are loaded with a write lock, in a deterministic order by
     * account number. Ordering the locks is what prevents two opposing transfers
     * (A to B and B to A) from deadlocking against each other.
     *
     * <p>The debit runs before the credit, so an insufficient-funds failure
     * aborts the transaction before any money is created.
     *
     * <p>Every attempt is audited, successful or not. The success entry is not
     * written here and now: the injected logger defers it until the transaction
     * actually commits, so a transfer that rolls back after this point can never
     * leave a SUCCESS entry behind. Failures are written immediately, because a
     * refused attempt is exactly what an investigator needs to see.
     */
    @Override
    @Transactional
    public void transfer(TransferCommand command) {
        Map<String, String> context = transferContext(command);

        if (command.from().equals(command.to())) {
            audit.failure(ACTION_TRANSFER, RESOURCE, command.from().value(), "SAME_ACCOUNT_TRANSFER", context);
            throw new SameAccountTransferException(command.from().value());
        }

        try {
            boolean fromFirst = command.from().value().compareTo(command.to().value()) < 0;
            AccountNumber firstLock = fromFirst ? command.from() : command.to();
            AccountNumber secondLock = fromFirst ? command.to() : command.from();

            Account first = loadForUpdate(firstLock);
            Account second = loadForUpdate(secondLock);

            Account source = fromFirst ? first : second;
            Account target = fromFirst ? second : first;

            source.debit(command.amount());
            target.credit(command.amount());

            repository.save(source);
            repository.save(target);

            audit.success(ACTION_TRANSFER, RESOURCE, command.from().value(), context);
        } catch (DomainException ex) {
            audit.failure(ACTION_TRANSFER, RESOURCE, command.from().value(), ex.code(), context);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> findByCustomerNumber(String customerNumber) {
        return repository.findByCustomerNumber(customerNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getByAccountNumber(AccountNumber accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber.value()));
    }

    private Map<String, String> transferContext(TransferCommand command) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("from", command.from().value());
        context.put("to", command.to().value());
        context.put("amount", command.amount().amount().toPlainString());
        context.put("currency", command.amount().currency().getCurrencyCode());
        return context;
    }

    private Account loadForUpdate(AccountNumber accountNumber) {
        return repository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber.value()));
    }
}
