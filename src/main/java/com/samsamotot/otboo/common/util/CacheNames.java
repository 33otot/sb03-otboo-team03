package com.samsamotot.otboo.common.util;

/**
 * 애플리케이션에서 사용하는 캐시 이름을 상수로 정의하는 클래스입니다.
 * <p>
 * 캐시 이름을 중앙에서 관리하여 오타를 방지하고,
 * 어떤 종류의 캐시가 사용되는지 명확하게 파악할 수 있습니다.
 * </p>
 */

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CacheNames {

    public static final String WEATHER = "weather";
    public static final String PROFILE = "profile";
    public static final String WEATHER_DAILY = "weather_daily";
}
