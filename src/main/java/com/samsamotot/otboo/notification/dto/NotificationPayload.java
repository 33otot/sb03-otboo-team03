package com.samsamotot.otboo.notification.dto;

import com.samsamotot.otboo.notification.entity.NotificationLevel;

import java.time.Instant;
import java.util.UUID;
/**
 * PackageName  : com.samsamotot.otboo.notification.dto
 * FileName     : NotificationPayload
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */



public record NotificationPayload(
    UUID id,
    UUID receiverId,
    String title,
    String content,
    NotificationLevel level,
    Instant createdAt
) {}
