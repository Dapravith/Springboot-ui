package com.springboot.account.domain.port.out;

import com.springboot.account.domain.model.Account;

/**
 * Outbound port for publishing account domain events.
 *
 * <p>Implementations must not propagate broker failures.
 */
public interface AccountEventPublisherPort {

    void accountOpened(Account account);
}
