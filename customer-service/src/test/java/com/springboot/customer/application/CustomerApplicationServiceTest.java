package com.springboot.customer.application;

import com.springboot.common.audit.RecordingAuditLogger;
import com.springboot.customer.domain.model.Customer;
import com.springboot.customer.domain.model.CustomerNotFoundException;
import com.springboot.customer.domain.model.CustomerNumber;
import com.springboot.customer.domain.model.DuplicateEmailException;
import com.springboot.customer.domain.port.in.RegisterCustomerUseCase.RegisterCustomerCommand;
import com.springboot.customer.domain.port.out.CustomerEventPublisherPort;
import com.springboot.customer.domain.port.out.CustomerNumberGeneratorPort;
import com.springboot.customer.domain.port.out.CustomerRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerApplicationServiceTest {

    private final FakeRepository repository = new FakeRepository();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final CustomerNumberGeneratorPort numbers = new SequentialNumbers();

    private final RecordingAuditLogger audit = new RecordingAuditLogger();

    private final CustomerApplicationService service =
            new CustomerApplicationService(repository, numbers, publisher, audit);

    @Test
    void registersAndPublishesAnEvent() {
        Customer customer = service.register(new RegisterCustomerCommand("Ada Lovelace", "ada@example.com"));

        assertEquals("Ada Lovelace", customer.fullName());
        assertEquals(1, publisher.published.size());
        assertEquals(customer.customerNumber(), publisher.published.getFirst().customerNumber());
    }

    @Test
    void normalisesEmailToLowercase() {
        Customer customer = service.register(new RegisterCustomerCommand("Ada", "  Ada@Example.COM  "));

        assertEquals("ada@example.com", customer.email(),
                "normalising at the boundary is what makes the duplicate check trustworthy");
    }

    @Test
    void refusesDuplicateEmail() {
        service.register(new RegisterCustomerCommand("Ada", "ada@example.com"));

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> service.register(new RegisterCustomerCommand("Someone Else", "ADA@example.com")));

        assertEquals("DUPLICATE_EMAIL", ex.code());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void duplicateRegistrationPublishesNoEvent() {
        service.register(new RegisterCustomerCommand("Ada", "ada@example.com"));
        publisher.published.clear();

        assertThrows(DuplicateEmailException.class,
                () -> service.register(new RegisterCustomerCommand("Ada Again", "ada@example.com")));

        assertTrue(publisher.published.isEmpty(), "a refused registration must not emit an event");
    }

    @Test
    void successfulRegistrationIsAudited() {
        Customer customer = service.register(new RegisterCustomerCommand("Ada", "ada@example.com"));

        assertEquals(1, audit.successes().size());
        RecordingAuditLogger.Entry entry = audit.withAction("CUSTOMER_REGISTERED").getFirst();
        assertEquals("Customer", entry.resourceType());
        assertEquals(customer.customerNumber().value(), entry.resourceId());
    }

    @Test
    void refusedRegistrationIsAuditedAsFailure() {
        service.register(new RegisterCustomerCommand("Ada", "ada@example.com"));
        audit.clear();

        assertThrows(DuplicateEmailException.class,
                () -> service.register(new RegisterCustomerCommand("Ada Again", "ada@example.com")));

        assertEquals(1, audit.failures().size(),
                "a refused attempt must still reach the audit trail");
        assertEquals("DUPLICATE_EMAIL", audit.failures().getFirst().reason());
        assertTrue(audit.successes().isEmpty());
    }

    @Test
    void unknownCustomerNumberIsReportedAsNotFound() {
        assertThrows(CustomerNotFoundException.class,
                () -> service.getByCustomerNumber(CustomerNumber.of("CUS9999999999")));
    }

    @Test
    void rejectsBlankInputAtTheCommandBoundary() {
        assertThrows(IllegalArgumentException.class, () -> new RegisterCustomerCommand("  ", "a@b.com"));
        assertThrows(IllegalArgumentException.class, () -> new RegisterCustomerCommand("Ada", "  "));
    }

    private static final class SequentialNumbers implements CustomerNumberGeneratorPort {
        private int counter = 0;

        @Override
        public CustomerNumber next() {
            return CustomerNumber.of("CUS%010d".formatted(++counter));
        }
    }

    private static final class RecordingPublisher implements CustomerEventPublisherPort {
        private final List<Customer> published = new ArrayList<>();

        @Override
        public void customerRegistered(Customer customer) {
            published.add(customer);
        }
    }

    private static final class FakeRepository implements CustomerRepositoryPort {
        private final Map<String, Customer> store = new LinkedHashMap<>();

        @Override
        public Customer save(Customer customer) {
            store.put(customer.customerNumber().value(), customer);
            return customer;
        }

        @Override
        public Optional<Customer> findByCustomerNumber(CustomerNumber customerNumber) {
            return Optional.ofNullable(store.get(customerNumber.value()));
        }

        @Override
        public List<Customer> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.values().stream().anyMatch(c -> c.email().equalsIgnoreCase(email));
        }
    }
}
