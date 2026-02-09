package com.samsamotot.otboo.weather.dto.event;

import com.samsamotot.otboo.notification.entity.NotificationLevel;

import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.weather.dto.event
 * FileName     : WeatherNotificationEvent
 * Description  : 날씨 변화 감지 시 발송되는 알림 이벤트
 */
public record WeatherNotificationEvent(
        List<UUID> receiverIds,
        String title,
        String content,
        NotificationLevel level
) { }
