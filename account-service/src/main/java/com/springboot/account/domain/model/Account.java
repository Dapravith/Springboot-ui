package com.springboot.account.domain.model;

import com.springboot.common.domain.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * Account aggregate root. Owns every rule about how a balance may change.
 *
 * <p>Framework-free by design. The invariants below are the reason this class
 * exists: no caller can move money without going through {@link #debit} and
 * {@link #credit}, so "balance never goes negative" and "a closed account never
 * moves" cannot be bypassed by a new adapter or a future controller.
 */
public final class Account {

    private final AccountNumber accountNumber;
    private final String customerNumber;
    private final Instant openedAt;
    private Money balance;
    private AccountStatus status;

    private Account(AccountNumber accountNumber, String customerNumber, Money balance, AccountStatus status,
                    Instant openedAt) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.customerNumber = Objects.requireNonNull(customerNumber, "customerNumber");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.status = Objects.requireNonNull(status, "status");
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
    }

    public static Account open(AccountNumber accountNumber, String customerNumber, Money openingBalance) {
        if (openingBalance.isNegative()) {
            throw new NegativeOpeningBalanceException(openingBalance.toString());
        }
        return new Account(accountNumber, customerNumber, openingBalance, AccountStatus.ACTIVE, Instant.now());
    }

    /** Rebuilds an account from storage without re-running the opening rules. */
    public static Account rehydrate(AccountNumber accountNumber, String customerNumber, Money balance,
                                    AccountStatus status, Instant openedAt) {
        return new Account(accountNumber, customerNumber, balance, status, openedAt);
    }

    public void debit(Money amount) {
        requireOperable(amount);
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(accountNumber.value(), balance.toString(), amount.toString());
        }
        this.balance = balance.minus(amount);
    }

    public void credit(Money amount) {
        requireOperable(amount);
        this.balance = balance.plus(amount);
    }

    public void freeze() {
        this.status = AccountStatus.FROZEN;
    }

    public void close() {
        this.status = AccountStatus.CLOSED;
    }

    public boolean canDebit(Money amount) {
        return status == AccountStatus.ACTIVE
                && balance.isSameCurrency(amount)
                && amount.isPositive()
                && !balance.isLessThan(amount);
    }

    private void requireOperable(Money amount) {
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new NonPositiveAmountException(amount.toString());
        }
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(accountNumber.value(), status.name());
        }
        // Money.minus/plus would also catch this, but failing here names the account.
        if (!balance.isSameCurrency(amount)) {
            throw new AccountCurrencyMismatchException(
                    accountNumber.value(), balance.currency().getCurrencyCode(),
                    amount.currency().getCurrencyCode());
        }
    }

    public AccountNumber accountNumber() {
        return accountNumber;
    }

    public String customerNumber() {
        return customerNumber;
    }

    public Money balance() {
        return balance;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant openedAt() {
        return openedAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Account a && accountNumber.equals(a.accountNumber);
    }

    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }
}
