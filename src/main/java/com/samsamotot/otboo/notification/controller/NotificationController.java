package com.samsamotot.otboo.notification.controller;

import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import com.samsamotot.otboo.notification.dto.NotificationRequest;
import com.samsamotot.otboo.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.controller
 * FileName     : NotificationController
 * Author       : dounguk
 * Date         : 2025. 9. 30.
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("api/notifications")
public class NotificationController{
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit
    ) {
        NotificationRequest request = new NotificationRequest(cursor, idAfter, limit);
        return ResponseEntity.ok().body(notificationService.getNotifications(request));
    }
}
