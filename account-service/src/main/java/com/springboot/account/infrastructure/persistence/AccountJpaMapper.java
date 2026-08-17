package com.springboot.account.infrastructure.persistence;

import com.springboot.account.domain.model.Account;
import com.springboot.account.domain.model.AccountNumber;
import com.springboot.account.domain.model.AccountStatus;
import com.springboot.common.domain.Money;

/** Translates between the domain model and the persistence entity. */
final class AccountJpaMapper {

    private AccountJpaMapper() {
    }

    static AccountJpaEntity toEntity(Account account) {
        return new AccountJpaEntity(
                account.accountNumber().value(),
                account.customerNumber(),
                account.balance().amount(),
                account.balance().currency().getCurrencyCode(),
                account.status().name(),
                account.openedAt());
    }

    static Account toDomain(AccountJpaEntity entity) {
        return Account.rehydrate(
                AccountNumber.of(entity.getAccountNumber()),
                entity.getCustomerNumber(),
                Money.of(entity.getBalanceAmount(), entity.getBalanceCurrency()),
                AccountStatus.valueOf(entity.getStatus()),
                entity.getOpenedAt());
    }
}
