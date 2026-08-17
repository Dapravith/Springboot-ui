package com.springboot.notification.interfaces.rest;

import com.springboot.common.web.ApiResponse;
import com.springboot.notification.domain.model.Notification;
import com.springboot.notification.domain.port.in.QueryNotificationUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/notifications", produces = "application/json")
public class NotificationController {

    private final QueryNotificationUseCase queryNotification;

    public NotificationController(QueryNotificationUseCase queryNotification) {
        this.queryNotification = queryNotification;
    }

    @GetMapping
    public ApiResponse<List<Notification>> recent() {
        return ApiResponse.ok(queryNotification.recent());
    }

    @GetMapping("/count")
    public ApiResponse<Integer> count() {
        return ApiResponse.ok(queryNotification.count());
    }
}
