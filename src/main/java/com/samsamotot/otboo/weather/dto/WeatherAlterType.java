package com.samsamotot.otboo.weather.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WeatherAlterType {

    // 온도
    TEMPERATURE_CHANGE("기온 변화 알림 🌡️"),

    // 습도 변화
    HUMIDITY_CHANGE("습도 변화 알림 💦"),

    // 하늘 상태 변화 (맑음 -> 흐림 등)
    SKY_STATUS_CHANGE("하늘 상태 변화 알림 ☁️"),

    // 강수 상태 변화 (비 안옴 -> 비 옴 등)
    PRECIPITATION_CHANGE("강수 예보 ☔️"),

    // 바람 상태 변화
    WIND_CHANGE("바람 상태 변화 알림 💨");

    private final String title;
}
