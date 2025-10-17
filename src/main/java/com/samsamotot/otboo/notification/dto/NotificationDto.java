package com.samsamotot.otboo.notification.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.entity.NotificationLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto
 * FileName     : NotificationDto
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 */
@Data
@Builder
@NoArgsConstructor
public class NotificationDto {
    private UUID id;
    private Instant createdAt;
    private UUID receiverId;
    private String title;
    private String content;
    private NotificationLevel level; // INFO, WARNING, ERROR

    @JsonCreator
    public NotificationDto(
            @JsonProperty("id") UUID id,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("receiverId") UUID receiverId,
            @JsonProperty("title") String title,
            @JsonProperty("content") String content,
            @JsonProperty("level") NotificationLevel level) {
        this.id = id;
        this.createdAt = createdAt;
        this.receiverId = receiverId;
        this.title = title;
        this.content = content;
        this.level = level;
    }

    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
            .id(notification.getId())
            .createdAt(notification.getCreatedAt())
            .receiverId(notification.getReceiver().getId())
            .title(notification.getTitle())
            .content(notification.getContent())
            .level(notification.getLevel())
            .build();
    }
}
