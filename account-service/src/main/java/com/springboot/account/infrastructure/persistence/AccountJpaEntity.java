package com.springboot.account.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persistence representation of an account.
 *
 * <p>Money is stored as an amount plus a currency code rather than as an
 * embedded type, keeping the column layout obvious and queryable.
 *
 * <p>The {@code @Version} column gives optimistic locking on top of the
 * pessimistic read lock used during transfers: the lock serialises concurrent
 * transfers, the version catches anything that slipped through another path.
 */
@Entity
@Table(name = "account")
public class AccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq_gen")
    @SequenceGenerator(name = "account_seq_gen", sequenceName = "account_seq", allocationSize = 50)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "customer_number", nullable = false, length = 20)
    private String customerNumber;

    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAmount;

    @Column(name = "balance_currency", nullable = false, length = 3)
    private String balanceCurrency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected AccountJpaEntity() {
        // for JPA
    }

    AccountJpaEntity(String accountNumber, String customerNumber, BigDecimal balanceAmount, String balanceCurrency,
                     String status, Instant openedAt) {
        this.accountNumber = accountNumber;
        this.customerNumber = customerNumber;
        this.balanceAmount = balanceAmount;
        this.balanceCurrency = balanceCurrency;
        this.status = status;
        this.openedAt = openedAt;
    }

    /** Applies a state change from the domain onto an already-managed row. */
    void apply(BigDecimal balanceAmount, String balanceCurrency, String status) {
        this.balanceAmount = balanceAmount;
        this.balanceCurrency = balanceCurrency;
        this.status = status;
    }

    String getAccountNumber() {
        return accountNumber;
    }

    String getCustomerNumber() {
        return customerNumber;
    }

    BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    String getBalanceCurrency() {
        return balanceCurrency;
    }

    String getStatus() {
        return status;
    }

    Instant getOpenedAt() {
        return openedAt;
    }

    long getVersion() {
        return version;
    }
}
