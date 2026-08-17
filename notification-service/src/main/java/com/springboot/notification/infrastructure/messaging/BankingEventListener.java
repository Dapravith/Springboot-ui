package com.springboot.notification.infrastructure.messaging;

import com.springboot.notification.domain.port.in.RecordNotificationUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Driving adapter for Kafka. Consumes events published by customer-service and
 * account-service and hands them to the use case; it holds no logic itself.
 *
 * <p>Disabled when notification.kafka.enabled=false so the service can run
 * without a broker during local development.
 */
@Component
@ConditionalOnProperty(prefix = "notification.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
class BankingEventListener {

    private final RecordNotificationUseCase recordNotification;

    BankingEventListener(RecordNotificationUseCase recordNotification) {
        this.recordNotification = recordNotification;
    }

    @KafkaListener(
            topics = {"#{'${notification.kafka.topics}'.split(',')}"},
            groupId = "${notification.kafka.group-id}")
    public void onEvent(@Payload String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        recordNotification.record(topic, payload);
    }
}
