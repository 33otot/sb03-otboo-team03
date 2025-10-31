package com.samsamotot.otboo.weather.dto.event;

/**
 * 날씨 업데이트 요청 위치 정보를 담는 이벤트 DTO
 * @param x 격자 X 좌표
 * @param y 격자 Y 좌표
 */
public record WeatherUpdateEvent(
        int x,
        int y
) {
}
