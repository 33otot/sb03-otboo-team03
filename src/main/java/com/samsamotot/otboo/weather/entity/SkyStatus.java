package com.samsamotot.otboo.weather.entity;

public enum SkyStatus {
    CLEAR,              // 맑음
    MOSTLY_CLOUDY,      // 구름많음
    CLOUDY;              // 흐림

    /* API 코드값(String)을 Enum으로 변환하는 정적 메소드 */
    public static SkyStatus fromCode(String code) {
        return switch (code) {
            case "1" -> CLEAR;
            case "3" -> MOSTLY_CLOUDY;
            case "4" -> CLOUDY;
            default -> null;
        };
    }
}
