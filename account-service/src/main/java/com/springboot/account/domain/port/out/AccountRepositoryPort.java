package com.springboot.account.domain.port.out;

import com.springboot.account.domain.model.Account;
import com.springboot.account.domain.model.AccountNumber;

import java.util.List;
import java.util.Optional;

/** Outbound port for account persistence. */
public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findByAccountNumber(AccountNumber accountNumber);

    /**
     * Loads an account for update, taking a pessimistic write lock.
     *
     * <p>Used by the transfer use case: two concurrent transfers touching the
     * same account must serialise, not interleave and lose an update.
     */
    Optional<Account> findByAccountNumberForUpdate(AccountNumber accountNumber);

    List<Account> findAll();

    List<Account> findByCustomerNumber(String customerNumber);
}
