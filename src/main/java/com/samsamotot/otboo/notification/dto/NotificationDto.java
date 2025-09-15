package com.samsamotot.otboo.notification.dto;

import com.samsamotot.otboo.notification.entity.NotificationLevel;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto
 * FileName     : NotificationDto
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Builder
public record NotificationDto(
    UUID id,
    Instant createdAt,
    UUID receiverId,
    String title,
    String content,
    NotificationLevel level
) {

}
