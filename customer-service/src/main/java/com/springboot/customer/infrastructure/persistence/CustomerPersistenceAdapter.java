package com.springboot.customer.infrastructure.persistence;

import com.springboot.customer.domain.model.Customer;
import com.springboot.customer.domain.model.CustomerNumber;
import com.springboot.customer.domain.port.out.CustomerRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Driven adapter implementing the domain's persistence port with JPA. */
@Component
class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository jpaRepository;

    CustomerPersistenceAdapter(CustomerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity saved = jpaRepository.save(CustomerJpaMapper.toEntity(customer));
        return CustomerJpaMapper.toDomain(saved);
    }

    @Override
    public Optional<Customer> findByCustomerNumber(CustomerNumber customerNumber) {
        return jpaRepository.findByCustomerNumber(customerNumber.value())
                .map(CustomerJpaMapper::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return jpaRepository.findAll().stream()
                .map(CustomerJpaMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
