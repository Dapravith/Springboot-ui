package com.springboot.notification.infrastructure.persistence;

import com.springboot.notification.domain.model.Notification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedNotificationStoreTest {

    @Test
    void evictsOldestBeyondCapacity() {
        BoundedNotificationStore store = new BoundedNotificationStore(3);

        for (int i = 1; i <= 10; i++) {
            store.add(Notification.received("t", "E", "event-" + i));
        }

        assertEquals(3, store.size(), "the store must stay bounded against an unbounded stream");
        assertEquals("event-10", store.recent().getFirst().payload());
        assertTrue(store.recent().stream().noneMatch(n -> n.payload().equals("event-1")));
    }

    @Test
    void startsEmpty() {
        assertEquals(0, new BoundedNotificationStore(5).size());
    }
}
