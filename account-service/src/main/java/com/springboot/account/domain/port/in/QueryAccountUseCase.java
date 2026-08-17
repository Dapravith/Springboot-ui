package com.springboot.account.domain.port.in;

import com.springboot.account.domain.model.Account;
import com.springboot.account.domain.model.AccountNumber;

import java.util.List;

/** Inbound port: read-side access to accounts. */
public interface QueryAccountUseCase {

    List<Account> findAll();

    List<Account> findByCustomerNumber(String customerNumber);

    /**
     * @throws com.springboot.account.domain.model.AccountNotFoundException if absent
     */
    Account getByAccountNumber(AccountNumber accountNumber);
}
