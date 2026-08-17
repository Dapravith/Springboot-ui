package com.springboot.account.domain.model;

import com.springboot.common.domain.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the invariants that make the aggregate worth having. No Spring
 * context, no database - the domain is framework-free, so its rules are cheap
 * to test exhaustively.
 */
class AccountTest {

    private static final AccountNumber NUMBER = AccountNumber.of("ACC000000000001");
    private static final String CUSTOMER = "CUS0000000001";

    private Account accountWith(String balance) {
        return Account.open(NUMBER, CUSTOMER, Money.of(balance, "USD"));
    }

    @Test
    void opensActiveWithGivenBalance() {
        Account account = accountWith("100.00");

        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(Money.of("100.00", "USD"), account.balance());
    }

    @Test
    void refusesNegativeOpeningBalance() {
        assertThrows(NegativeOpeningBalanceException.class,
                () -> Account.open(NUMBER, CUSTOMER, Money.of("-0.01", "USD")));
    }

    @Test
    void debitReducesAndCreditIncreases() {
        Account account = accountWith("100.00");

        account.debit(Money.of("40.00", "USD"));
        assertEquals(Money.of("60.00", "USD"), account.balance());

        account.credit(Money.of("15.50", "USD"));
        assertEquals(Money.of("75.50", "USD"), account.balance());
    }

    @Test
    void debitOfEntireBalanceIsAllowed() {
        Account account = accountWith("100.00");

        assertTrue(account.canDebit(Money.of("100.00", "USD")));
        account.debit(Money.of("100.00", "USD"));

        assertEquals(Money.zero("USD"), account.balance());
    }

    @Test
    void balanceCannotGoNegative() {
        Account account = accountWith("100.00");

        assertFalse(account.canDebit(Money.of("100.01", "USD")));
        assertThrows(InsufficientFundsException.class, () -> account.debit(Money.of("100.01", "USD")));
        assertEquals(Money.of("100.00", "USD"), account.balance(),
                "a rejected debit must leave the balance untouched");
    }

    @Test
    void nonActiveAccountCannotMoveMoney() {
        Account frozen = accountWith("100.00");
        frozen.freeze();

        assertThrows(AccountNotActiveException.class, () -> frozen.debit(Money.of("1.00", "USD")));
        assertThrows(AccountNotActiveException.class, () -> frozen.credit(Money.of("1.00", "USD")));
        assertFalse(frozen.canDebit(Money.of("1.00", "USD")));
    }

    @Test
    void closedAccountCannotMoveMoney() {
        Account closed = accountWith("100.00");
        closed.close();

        assertEquals(AccountStatus.CLOSED, closed.status());
        assertThrows(AccountNotActiveException.class, () -> closed.credit(Money.of("1.00", "USD")));
    }

    @Test
    void refusesZeroAndNegativeAmounts() {
        Account account = accountWith("100.00");

        assertThrows(NonPositiveAmountException.class, () -> account.debit(Money.zero("USD")));
        assertThrows(NonPositiveAmountException.class, () -> account.credit(Money.of("-5.00", "USD")));
    }

    @Test
    void refusesForeignCurrencyMovement() {
        Account account = accountWith("100.00");

        assertThrows(AccountCurrencyMismatchException.class, () -> account.debit(Money.of("1.00", "EUR")));
        assertFalse(account.canDebit(Money.of("1.00", "EUR")));
    }

    @Test
    void identityIsTheAccountNumber() {
        Account a = accountWith("100.00");
        Account b = accountWith("999.00");

        assertEquals(a, b, "same account number means the same account, whatever the balance");
        assertEquals(a.hashCode(), b.hashCode());
    }
}
