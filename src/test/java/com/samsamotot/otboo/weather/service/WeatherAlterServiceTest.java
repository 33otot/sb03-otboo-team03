package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.common.fixture.*;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import com.samsamotot.otboo.weather.service.impl.WeatherAlterServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class WeatherAlterServiceTest {

    @InjectMocks
    private WeatherAlterServiceImpl weatherAlterService;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void 기온_하강과_강수_변화_감지_시_2개의_알림_발송() {
        Grid grid = GridFixture.createGrid();

        // Given: 테스트 데이터 준비
        Weather newWeather = WeatherFixture.createWeather(grid);
        Weather previousWeather = WeatherFixture.previousWeather(
                grid,
                newWeather.getForecastedAt().minus(1, ChronoUnit.DAYS),
                newWeather.getForecastAt());
        Location location = LocationFixture.createLocation(grid);

        previousWeather.setSkyStatus(SkyStatus.CLEAR);
        previousWeather.setPrecipitationType(Precipitation.NONE);
        previousWeather.setTemperatureComparedToDayBefore(0.0);

        // 특별한 날씨 변화 감지
        newWeather.setTemperatureComparedToDayBefore(-6.0); // -5도 이상 변화
        newWeather.setPrecipitationType(Precipitation.RAIN); // 맑음 -> 비

        User user = UserFixture.createUser();
        Profile profile = ProfileFixture.createLocationProfile(user, location);
        List<User> usersToNotify = List.of(user);

        // Mock 설정
        given(weatherRepository.findByGridAndForecastedAtAndForecastAt(any(), any(), any()))
                .willReturn(Optional.of(previousWeather));
        given(profileRepository.findAllByLocationGridId(any()))
                .willReturn(List.of(profile));

        // When: 테스트할 메소드 실행
        weatherAlterService.checkAndSendAlerts(newWeather);

        // Then: 결과 검증
        // 2개의 알림(기온, 강수)이 각각 발송되었는지 확인
        verify(notificationService, times(2)).save(any(), anyString(), anyString(), any());
    }

    @Test
    void 비교할_어제_날씨_데이터_없으면_알림_없음() {
        Grid grid = GridFixture.createGrid();

        // Given
        Weather newWeather = WeatherFixture.createWeather(grid);
        given(weatherRepository.findByGridAndForecastedAtAndForecastAt(any(), any(), any()))
                .willReturn(Optional.empty());

        // When
        weatherAlterService.checkAndSendAlerts(newWeather);

        // Then
        // 어떤 알림 로직도 호출되지 않아야 함
        verify(notificationService, never()).save(any(), anyString(), anyString(), any());
        verify(profileRepository, never()).findAllByLocationGridId(any());
    }

    @Test
    void 의미_있는_날씨_변화_없으면_알림_없음() {
        Grid grid = GridFixture.createGrid();

        // Given
        Weather newWeather = WeatherFixture.createWeather(grid);
        Weather previousWeather = WeatherFixture.previousWeather(
                grid,
                newWeather.getForecastedAt().minus(1, ChronoUnit.DAYS),
                newWeather.getForecastAt());

        // 특별한 날씨 변화 없음
        newWeather.setTemperatureComparedToDayBefore(1.0); // 5도 미만 변화
        newWeather.setPrecipitationType(Precipitation.NONE); // 변화 없음

        given(weatherRepository.findByGridAndForecastedAtAndForecastAt(any(), any(), any()))
                .willReturn(Optional.of(previousWeather));

        // When
        weatherAlterService.checkAndSendAlerts(newWeather);

        // Then
        verify(notificationService, never()).save(any(), anyString(), anyString(), any());
    }

    @Test
    void 날씨_변화_체크했지만_해당_지역에_사용자가_없으면_알림_없음() {
        Grid grid = GridFixture.createGrid();

        // Given
        Weather newWeather = WeatherFixture.createWeather(grid);
        Weather previousWeather = WeatherFixture.previousWeather(
                grid,
                newWeather.getForecastedAt().minus(1, ChronoUnit.DAYS),
                newWeather.getForecastAt());

        newWeather.setPrecipitationType(Precipitation.RAIN); // 변화 발생
        previousWeather.setPrecipitationType(Precipitation.NONE);

        given(weatherRepository.findByGridAndForecastedAtAndForecastAt(any(), any(), any()))
                .willReturn(Optional.of(previousWeather));
        // 해당 지역에 사용자가 없음
        given(profileRepository.findAllByLocationGridId(any()))
                .willReturn(Collections.emptyList());

        // When
        weatherAlterService.checkAndSendAlerts(newWeather);

        // Then
        verify(notificationService, never()).save(any(), anyString(), anyString(), any());
    }
}