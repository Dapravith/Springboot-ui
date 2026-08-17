package com.springboot.notification.infrastructure.persistence;

import com.springboot.notification.domain.model.Notification;
import com.springboot.notification.domain.port.out.NotificationStorePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Fixed-capacity, newest-first ring of recent notifications.
 *
 * <p>The bound is the point: this service consumes an unbounded stream, so the
 * oldest entry is evicted rather than letting the heap grow until the JVM dies.
 */
@Component
class BoundedNotificationStore implements NotificationStorePort {

    private final Deque<Notification> notifications = new ArrayDeque<>();
    private final int maxEntries;

    BoundedNotificationStore(@Value("${notification.store.max-entries:200}") int maxEntries) {
        this.maxEntries = maxEntries;
    }

    @Override
    public synchronized void add(Notification notification) {
        while (notifications.size() >= maxEntries) {
            notifications.removeLast();
        }
        notifications.addFirst(notification);
    }

    @Override
    public synchronized List<Notification> recent() {
        return List.copyOf(notifications);
    }

    @Override
    public synchronized int size() {
        return notifications.size();
    }
}
