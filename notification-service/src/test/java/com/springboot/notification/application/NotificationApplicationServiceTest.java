package com.springboot.notification.application;

import com.springboot.common.audit.RecordingAuditLogger;
import com.springboot.notification.domain.model.Notification;
import com.springboot.notification.domain.port.out.NotificationStorePort;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationApplicationServiceTest {

    private final FakeStore store = new FakeStore(3);
    private final RecordingAuditLogger audit = new RecordingAuditLogger();
    private final NotificationApplicationService service = new NotificationApplicationService(store, audit);

    @Test
    void extractsEventTypeFromPayload() {
        service.record("customer-registered-topic",
                "{\"eventType\":\"CustomerRegistered\",\"customerNumber\":\"CUS0000000001\"}");

        assertEquals("CustomerRegistered", store.recent().getFirst().eventType());
    }

    @Test
    void retainsMalformedPayloadsRatherThanDroppingThem() {
        service.record("account-opened-topic", "not json at all");

        assertEquals(1, service.count(), "an operator needs to see what actually arrived");
        assertEquals("Unknown", store.recent().getFirst().eventType());
    }

    @Test
    void handlesPayloadWithoutEventType() {
        service.record("account-opened-topic", "{\"accountNumber\":\"ACC000000000001\"}");

        assertEquals("Unknown", store.recent().getFirst().eventType());
    }

    @Test
    void newestNotificationIsReturnedFirst() {
        service.record("t", "{\"eventType\":\"First\"}");
        service.record("t", "{\"eventType\":\"Second\"}");

        assertEquals("Second", service.recent().getFirst().eventType());
    }

    @Test
    void consumptionIsAudited() {
        service.record("customer-registered-topic", "{\"eventType\":\"CustomerRegistered\"}");

        assertEquals(1, audit.withAction("EVENT_CONSUMED").size());
        assertEquals("customer-registered-topic",
                audit.withAction("EVENT_CONSUMED").getFirst().attributes().get("topic"));
    }

    private static final class FakeStore implements NotificationStorePort {
        private final Deque<Notification> items = new ArrayDeque<>();
        private final int max;

        FakeStore(int max) {
            this.max = max;
        }

        @Override
        public void add(Notification notification) {
            while (items.size() >= max) {
                items.removeLast();
            }
            items.addFirst(notification);
        }

        @Override
        public List<Notification> recent() {
            return List.copyOf(items);
        }

        @Override
        public int size() {
            return items.size();
        }
    }
}
