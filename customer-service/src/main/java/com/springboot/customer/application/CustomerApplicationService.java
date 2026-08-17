package com.springboot.customer.application;

import com.springboot.common.audit.AuditLogger;
import com.springboot.customer.domain.model.Customer;
import com.springboot.customer.domain.model.CustomerNotFoundException;
import com.springboot.customer.domain.model.CustomerNumber;
import com.springboot.customer.domain.model.DuplicateEmailException;
import com.springboot.customer.domain.port.in.QueryCustomerUseCase;
import com.springboot.customer.domain.port.in.RegisterCustomerUseCase;
import com.springboot.customer.domain.port.out.CustomerEventPublisherPort;
import com.springboot.customer.domain.port.out.CustomerNumberGeneratorPort;
import com.springboot.customer.domain.port.out.CustomerRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates the customer use cases.
 *
 * <p>This layer owns transaction boundaries, the ordering of calls to outbound
 * ports, and the audit trail. It holds no business rules of its own - those live
 * in the domain model - and it depends only on port interfaces, never on JPA,
 * Kafka or HTTP types.
 *
 * <p>Auditing lives here rather than in the controller because this is the layer
 * that knows the outcome. A refused registration is audited too: an audit trail
 * containing only successes cannot answer who tried what.
 */
@Service
public class CustomerApplicationService implements RegisterCustomerUseCase, QueryCustomerUseCase {

    private static final String RESOURCE = "Customer";
    private static final String ACTION_REGISTER = "CUSTOMER_REGISTERED";

    private final CustomerRepositoryPort repository;
    private final CustomerNumberGeneratorPort numberGenerator;
    private final CustomerEventPublisherPort eventPublisher;
    private final AuditLogger audit;

    public CustomerApplicationService(CustomerRepositoryPort repository,
                                      CustomerNumberGeneratorPort numberGenerator,
                                      CustomerEventPublisherPort eventPublisher,
                                      AuditLogger audit) {
        this.repository = repository;
        this.numberGenerator = numberGenerator;
        this.eventPublisher = eventPublisher;
        this.audit = audit;
    }

    @Override
    @Transactional
    public Customer register(RegisterCustomerCommand command) {
        if (repository.existsByEmail(command.email())) {
            audit.failure(ACTION_REGISTER, RESOURCE, command.email(), "DUPLICATE_EMAIL");
            throw new DuplicateEmailException(command.email());
        }

        Customer customer = Customer.register(numberGenerator.next(), command.fullName(), command.email());
        Customer saved = repository.save(customer);

        // Published after the write so the event never describes state that was
        // rolled back. The adapter swallows broker failures by contract.
        eventPublisher.customerRegistered(saved);

        audit.success(ACTION_REGISTER, RESOURCE, saved.customerNumber().value(),
                Map.of("email", saved.email(), "status", saved.status().name()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getByCustomerNumber(CustomerNumber customerNumber) {
        return repository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new CustomerNotFoundException(customerNumber.value()));
    }
}
