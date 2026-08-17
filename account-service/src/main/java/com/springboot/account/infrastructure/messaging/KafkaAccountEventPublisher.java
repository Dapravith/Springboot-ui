package com.springboot.account.infrastructure.messaging;

import com.springboot.account.domain.model.Account;
import com.springboot.account.domain.port.out.AccountEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Publishes account events to Kafka.
 *
 * <p>Honours the port contract that broker failures must not fail the business
 * operation: serialisation and send errors are logged, not rethrown.
 */
@Component
@ConditionalOnProperty(prefix = "account.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
class KafkaAccountEventPublisher implements AccountEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaAccountEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    KafkaAccountEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               @Value("${account.kafka.account-opened-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    /** Wire format of the event. Additive changes only, so consumers stay compatible. */
    record AccountOpenedEvent(String eventType, String accountNumber, String customerNumber,
                              BigDecimal openingBalance, String currency, Instant occurredAt) {
    }

    @Override
    public void accountOpened(Account account) {
        String accountNumber = account.accountNumber().value();

        AccountOpenedEvent event = new AccountOpenedEvent(
                "AccountOpened",
                accountNumber,
                account.customerNumber(),
                account.balance().amount(),
                account.balance().currency().getCurrencyCode(),
                Instant.now());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException ex) {
            log.error("Could not serialise AccountOpened event for {}", accountNumber, ex);
            return;
        }

        kafkaTemplate.send(topic, accountNumber, payload)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Failed to publish AccountOpened for {}", accountNumber, throwable);
                    } else {
                        log.info("Published AccountOpened for {} to {}", accountNumber, topic);
                    }
                });
    }
}
