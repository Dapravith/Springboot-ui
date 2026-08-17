package com.springboot.notification.domain.model;

import java.time.Instant;
import java.util.Objects;

/** A banking event that was received and retained for inspection. */
public record Notification(String topic, String eventType, String payload, Instant receivedAt) {

    public Notification {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }

    public static Notification received(String topic, String eventType, String payload) {
        return new Notification(topic, eventType, payload, Instant.now());
    }
}
