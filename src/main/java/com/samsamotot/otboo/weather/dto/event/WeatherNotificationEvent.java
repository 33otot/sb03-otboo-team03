package com.samsamotot.otboo.weather.dto.event;

import com.samsamotot.otboo.notification.entity.NotificationLevel;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto.event
 * FileName     : WeatherNotificationEvent
 * Description  : 날씨 변화 감지 시 발송되는 알림 이벤트
 */
public record WeatherNotificationEvent(
        UUID receiverId,
        String title,
        String content,
        NotificationLevel level
) {

}
