package com.samsamotot.otboo.weather.entity;

public enum WindAsWord {
    WEAK,           // 약한 바람 (4m/s 미만)
    MODERATE,       // 약간 강한 바람 (4m/s ~ 9m/s)
    STRONG;          // 강한 바람 (9m/s 이상)

    // 풍속(Double)을 정성 정보(Enum)로 변환하는 정적 메소드
    public static WindAsWord fromSpeed(Double speed) {
        if (speed < 4.0) {
            return WEAK;
        } else if (speed < 9.0) {
            return MODERATE;
        } else {
            return STRONG;
        }
    }
}
