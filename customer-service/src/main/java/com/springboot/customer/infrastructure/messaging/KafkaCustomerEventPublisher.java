package com.springboot.customer.infrastructure.messaging;

import com.springboot.customer.domain.model.Customer;
import com.springboot.customer.domain.port.out.CustomerEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Publishes customer events to Kafka.
 *
 * <p>Honours the port contract that broker failures must not fail the business
 * operation: serialisation and send errors are logged, not rethrown. Payloads
 * go through Jackson rather than string concatenation so a field value can
 * never break the JSON structure.
 *
 * <p>Note: Spring Boot 4 ships Jackson 3, whose types live under
 * {@code tools.jackson} and whose exceptions are unchecked.
 */
@Component
@ConditionalOnProperty(prefix = "customer.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
class KafkaCustomerEventPublisher implements CustomerEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaCustomerEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    KafkaCustomerEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper,
                                @Value("${customer.kafka.customer-registered-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    /** Wire format of the event. Additive changes only, so consumers stay compatible. */
    record CustomerRegisteredEvent(String eventType, String customerNumber, String fullName, String email,
                                   Instant occurredAt) {
    }

    @Override
    public void customerRegistered(Customer customer) {
        String customerNumber = customer.customerNumber().value();

        CustomerRegisteredEvent event = new CustomerRegisteredEvent(
                "CustomerRegistered", customerNumber, customer.fullName(), customer.email(), Instant.now());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException ex) {
            log.error("Could not serialise CustomerRegistered event for {}", customerNumber, ex);
            return;
        }

        kafkaTemplate.send(topic, customerNumber, payload)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Failed to publish CustomerRegistered for {}", customerNumber, throwable);
                    } else {
                        log.info("Published CustomerRegistered for {} to {}", customerNumber, topic);
                    }
                });
    }
}
