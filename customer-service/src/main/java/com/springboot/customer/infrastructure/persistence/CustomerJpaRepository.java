package com.springboot.customer.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data repository. Infrastructure detail; never referenced by the domain. */
interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, Long> {

    Optional<CustomerJpaEntity> findByCustomerNumber(String customerNumber);

    boolean existsByEmail(String email);
}
