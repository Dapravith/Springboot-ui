package com.springboot.notification.application;

import com.springboot.common.audit.AuditLogger;
import com.springboot.notification.domain.model.Notification;
import com.springboot.notification.domain.port.in.QueryNotificationUseCase;
import com.springboot.notification.domain.port.in.RecordNotificationUseCase;
import com.springboot.notification.domain.port.out.NotificationStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Records inbound banking events.
 *
 * <p>Extracting {@code eventType} is best-effort: a malformed or unexpected
 * payload must still be retained, because the whole point of this service is to
 * show an operator what actually arrived - including the messages that a
 * stricter consumer would have silently dropped.
 */
@Service
public class NotificationApplicationService implements RecordNotificationUseCase, QueryNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationApplicationService.class);

    private static final String RESOURCE = "BankingEvent";
    private static final String ACTION_CONSUMED = "EVENT_CONSUMED";

    private final NotificationStorePort store;
    private final AuditLogger audit;

    public NotificationApplicationService(NotificationStorePort store, AuditLogger audit) {
        this.store = store;
        this.audit = audit;
    }

    @Override
    public void record(String topic, String payload) {
        String eventType = extractEventType(payload);
        store.add(Notification.received(topic, eventType, payload));
        log.info("Recorded event from {}", topic);

        // Auditing consumption gives the trail a receiving side: without it, a
        // published event that never arrived looks identical to one that did.
        audit.success(ACTION_CONSUMED, RESOURCE, eventType, Map.of("topic", topic));
    }

    @Override
    public List<Notification> recent() {
        return store.recent();
    }

    @Override
    public int count() {
        return store.size();
    }

    /** Shallow scan rather than a full parse: never fails, never rejects a message. */
    private String extractEventType(String payload) {
        int key = payload.indexOf("\"eventType\"");
        if (key < 0) {
            return "Unknown";
        }
        int open = payload.indexOf('"', payload.indexOf(':', key) + 1);
        int close = open < 0 ? -1 : payload.indexOf('"', open + 1);
        return (open < 0 || close < 0) ? "Unknown" : payload.substring(open + 1, close);
    }
}
