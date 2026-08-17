package com.springboot.account.domain.port.out;

import com.springboot.account.domain.model.AccountNumber;

/** Outbound port for allocating account numbers. */
public interface AccountNumberGeneratorPort {

    AccountNumber next();
}
