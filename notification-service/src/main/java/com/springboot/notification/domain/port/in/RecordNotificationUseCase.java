package com.springboot.notification.domain.port.in;

/**
 * Inbound port: recording an inbound banking event.
 *
 * <p>Driven by the Kafka listener today. Because it is a port, a replay tool or
 * an HTTP ingest endpoint could drive the identical logic tomorrow.
 */
public interface RecordNotificationUseCase {

    void record(String topic, String payload);
}
