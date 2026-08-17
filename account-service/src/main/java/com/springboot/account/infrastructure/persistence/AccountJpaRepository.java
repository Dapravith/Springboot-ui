package com.springboot.account.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Spring Data repository. Infrastructure detail; never referenced by the domain. */
interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {

    Optional<AccountJpaEntity> findByAccountNumber(String accountNumber);

    List<AccountJpaEntity> findByCustomerNumber(String customerNumber);

    /**
     * SELECT ... FOR UPDATE. Serialises concurrent transfers that touch the same
     * row instead of letting them read-modify-write over one another.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountJpaEntity a where a.accountNumber = :accountNumber")
    Optional<AccountJpaEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
