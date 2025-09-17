package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.entity.WindAsWord;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * Weather 엔티티 생성을 위한 간단한 픽스처 유틸.
 * - 외부 라이브러리 없이 기본값을 채운 Weather 인스턴스를 만들어 줍니다.
 * - 필요한 필드만 덮어쓰고 싶다면 {@link #sample(Consumer)} 오버라이드를 사용하세요.
 */
public final class WeatherFixture {

    private WeatherFixture() {}

    /** 기본 Location 포함한 Weather 샘플 */
    public static Weather sample() {
        return builder(defaultLocation()).build();
    }

    /** 주어진 Location으로 Weather 샘플 */
    public static Weather sample(Location location) {
        return builder(location).build();
    }

    /** 커스터마이저로 일부 필드만 덮어쓴 Weather 샘플 */
    public static Weather sample(Consumer<Weather.WeatherBuilder> customizer) {
        Weather.WeatherBuilder builder = builder(defaultLocation());
        if (customizer != null) {
            customizer.accept(builder);
        }
        return builder.build();
    }

    /** Location 샘플(서울 기준 좌표) */
    public static Location defaultLocation() {
        return Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .x(60)
                .y(127)
                .locationNames(List.of("Seoul", "Jung-gu"))
                .build();
    }

    private static Weather.WeatherBuilder builder(Location location) {
        Instant now = Instant.now();
        return Weather.builder()
                .forecastAt(now)
                .forecastedAt(now)
                .location(location)
                .skyStatus(SkyStatus.CLEAR)
                .precipitationType(Precipitation.NONE)
                .precipitationAmount(0.0)
                .precipitationProbability(0.0)
                .humidityCurrent(50.0)
                .humidityComparedToDayBefore(0.0)
                .temperatureCurrent(20.0)
                .temperatureComparedToDayBefore(0.0)
                .temperatureMin(15.0)
                .temperatureMax(25.0)
                .windSpeed(1.5)
                .windAsWord(WindAsWord.WEAK);
    }
}


