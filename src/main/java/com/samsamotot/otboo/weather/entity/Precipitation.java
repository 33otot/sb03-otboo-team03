package com.samsamotot.otboo.weather.entity;

public enum Precipitation {
    NONE,               // 없음
    RAIN,               // 비
    RAIN_SNOW,          // 비/눈
    SNOW,               // 눈
    SHOWER;             // 소나기

    /* API 코드값(String)을 Enum으로 변환하는 정적 메소드 */
    public static Precipitation fromCode(String code) {
        return switch (code) {
            case "0" -> NONE;
            case "1" -> RAIN;
            case "2" -> RAIN_SNOW;
            case "3" -> SNOW;
            case "4" -> SHOWER;
            default -> NONE;
        };
    }
}
