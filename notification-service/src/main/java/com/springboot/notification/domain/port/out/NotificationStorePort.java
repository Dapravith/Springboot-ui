package com.springboot.notification.domain.port.out;

import com.springboot.notification.domain.model.Notification;

import java.util.List;

/**
 * Outbound port for notification retention.
 *
 * <p>Implementations must be bounded: this service consumes an unbounded event
 * stream, so an unbounded store would be a memory leak with a delay fuse.
 */
public interface NotificationStorePort {

    void add(Notification notification);

    List<Notification> recent();

    int size();
}
