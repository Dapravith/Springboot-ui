package com.springboot.notification.domain.port.in;

import com.springboot.notification.domain.model.Notification;

import java.util.List;

/** Inbound port: read-side access to recent notifications. */
public interface QueryNotificationUseCase {

    List<Notification> recent();

    int count();
}
