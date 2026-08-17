package com.springboot.account.infrastructure.persistence;

import com.springboot.account.domain.model.Account;
import com.springboot.account.domain.model.AccountNumber;
import com.springboot.account.domain.port.out.AccountRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Driven adapter implementing the domain's persistence port with JPA.
 *
 * <p>On save, an existing row is updated in place through
 * {@link AccountJpaEntity#apply} rather than replaced by a detached copy. That
 * keeps the managed entity - and therefore its {@code @Version} and its
 * pessimistic lock - intact for the remainder of the transaction.
 */
@Component
class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository jpaRepository;

    AccountPersistenceAdapter(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = jpaRepository.findByAccountNumber(account.accountNumber().value())
                .map(existing -> {
                    existing.apply(
                            account.balance().amount(),
                            account.balance().currency().getCurrencyCode(),
                            account.status().name());
                    return existing;
                })
                .orElseGet(() -> AccountJpaMapper.toEntity(account));

        return AccountJpaMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Account> findByAccountNumber(AccountNumber accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber.value()).map(AccountJpaMapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumberForUpdate(AccountNumber accountNumber) {
        return jpaRepository.findByAccountNumberForUpdate(accountNumber.value()).map(AccountJpaMapper::toDomain);
    }

    @Override
    public List<Account> findAll() {
        return jpaRepository.findAll().stream().map(AccountJpaMapper::toDomain).toList();
    }

    @Override
    public List<Account> findByCustomerNumber(String customerNumber) {
        return jpaRepository.findByCustomerNumber(customerNumber).stream().map(AccountJpaMapper::toDomain).toList();
    }
}
