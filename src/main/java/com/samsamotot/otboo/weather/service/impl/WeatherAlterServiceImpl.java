package com.samsamotot.otboo.weather.service.impl;

import com.samsamotot.otboo.notification.entity.NotificationLevel;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.dto.WeatherChangeDto;
import com.samsamotot.otboo.weather.dto.event.WeatherNotificationEvent;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.entity.WindAsWord;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import com.samsamotot.otboo.weather.service.WeatherAlterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PackageName  : com.samsamotot.otboo.weather.service.impl
 * FileName     : WeatherAlterServiceImpl
 * Author       : HuInDoL
 * Description  : 특별한 날씨 변화 시 알림
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherAlterServiceImpl implements WeatherAlterService {

    private static final String SERVICE_NAME = "[WeatherAlterServiceImpl] ";

    private static final double TEMP_STANDARD = 5.0;
    private static final double HUMID_STANDARD = 50.0;

    private final WeatherRepository weatherRepository;
    private final ProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 새로운 날씨 데이터를 기준으로 변화를 감지하고 알림을 발송하는 메인 메소드입니다.
     *
     * @param newWeather 새로 저장된 날씨 엔티티
     */
    @Override
    @Transactional(readOnly = false)
    public void checkAndSendAlerts(Weather newWeather) {
        log.info(SERVICE_NAME + "[알림 프로세스 시작] Grid ID: {}", newWeather.getGrid().getId());

        // 1. [준비] 이전 날씨 데이터 조회
        Optional<Weather> previousWeather = findPreviousWeather(newWeather);
        if (previousWeather.isEmpty()) {
            log.warn(SERVICE_NAME +  "[알림 중단] 비교할 어제 날씨 없음. newWeather forecastedAt: {}", newWeather.getForecastedAt());
            return;
        }

        // 2. [감지] 날씨 변화 감지
        WeatherChangeDto changes = detectChanges(previousWeather.get(), newWeather);
        log.info(SERVICE_NAME + "...[변화 감지 결과] {}", changes);
        if (!changes.hasChanges()) { // DTO의 hasChanges()
            return;
        }


        // 3. [대상 조회] 알림 대상 사용자 조회
        List<User> usersToNotify = findUsersToNotify(newWeather.getGrid().getId());
        if (usersToNotify.isEmpty()) {
            log.warn(SERVICE_NAME + "[알림 중단] 해당 지역에 알림을 받을 사용자가 없음.");
            return;
        }
        log.info(SERVICE_NAME + "...[통과] 알림 대상 사용자 {}명 찾음.", usersToNotify.size());

        // 4. [알림 발송]
        log.info(SERVICE_NAME + "[알림 발송 시도] 알림을 발송합니다.");

        sendNotifications(usersToNotify, changes);
    }

    /**
     * 현재 날씨 데이터와 비교할 '어제 같은 시각에 발표된' 날씨 데이터를 조회합니다.
     *
     * @param newWeather 현재 날씨 엔티티
     * @return Optional<Weather> 어제 발표된 날씨 데이터
     */
    private Optional<Weather> findPreviousWeather(Weather newWeather) {
        Instant previousForecastedAt = newWeather.getForecastedAt().minus(1, ChronoUnit.DAYS);

        return weatherRepository.findByGridAndForecastedAtAndForecastAt(
                newWeather.getGrid(),
                previousForecastedAt,         // 어제 예보된 시간
                newWeather.getForecastAt()  // 예보 대상 시간
        );
    }

    /**
     * 이전 날씨와 현재 날씨를 비교하여 의미 있는 변화가 있었는지 감지하고, 그 결과를 DTO에 담아 반환합니다.
     *
     * @param previousWeather 이전 날씨 엔티티
     * @param newWeather      현재 날씨 엔티티
     * @return WeatherChangeDto 변화된 날씨 정보만 담고 있는 DTO
     */
    private WeatherChangeDto detectChanges(Weather previousWeather, Weather newWeather) {
        Double tempComparedToDayBefore = null;
        Double humidComparedToDayBefore = null;
        SkyStatus skyStatusChange = null;
        Precipitation precipitationChange = null;
        WindAsWord windAsWordChange = null;

        // [기준 1]: 어제 대비 기온이 5도 이상 차이날 때
        Double tempCompared = newWeather.getTemperatureComparedToDayBefore();
        if (tempCompared != null && Math.abs(tempCompared) >= TEMP_STANDARD) {
            tempComparedToDayBefore = tempCompared;
        }

        // [기준 2]: 어제 대비 습도가 50%p 이상 차이날 때
        Double humidCompared = newWeather.getHumidityComparedToDayBefore();
        if (humidCompared != null && Math.abs(humidCompared) >= HUMID_STANDARD) {
            humidComparedToDayBefore = humidCompared;
        }

        // [기준 3]: 하늘 상태가 변경되었을 때 (맑음 -> 흐림 등)
        if (newWeather.getSkyStatus() != previousWeather.getSkyStatus()) {
            skyStatusChange = newWeather.getSkyStatus();
        }

        // [기준 4]: 강수 형태가 '없음'에서 '비', '비/눈', '눈', '소나기' 등으로 바뀌었을 때
        if (previousWeather.getPrecipitationType() == Precipitation.NONE &&
                (newWeather.getPrecipitationType() == Precipitation.RAIN ||
                        newWeather.getPrecipitationType() == Precipitation.SNOW ||
                        newWeather.getPrecipitationType() == Precipitation.RAIN_SNOW ||
                        newWeather.getPrecipitationType() == Precipitation.SHOWER)) {
            precipitationChange = newWeather.getPrecipitationType();
        }

        // [기준 5]: 바람이 '약한 바람'에서 '약간 강한 바람' 또는 '강한 바람'으로 바뀌었을 때
        if (previousWeather.getWindAsWord() == WindAsWord.WEAK &&
                (newWeather.getWindAsWord() == WindAsWord.MODERATE ||
                        newWeather.getWindAsWord() == WindAsWord.STRONG)) {
            windAsWordChange = newWeather.getWindAsWord();
        }

        return WeatherChangeDto.builder()
                .tempComparedToDayBefore(tempComparedToDayBefore)
                .humidComparedToDayBefore(humidComparedToDayBefore)
                .skyStatus(skyStatusChange)
                .precipitation(precipitationChange)
                .windAsWord(windAsWordChange)
                .build();
    }

    /**
     * 특정 격자(Grid)에 위치를 등록한 모든 사용자를 조회합니다.
     *
     * @param gridId 날씨 변화가 감지된 격자의 ID
     * @return List<User> 알림을 수신할 사용자 목록
     */
    private List<User> findUsersToNotify(UUID gridId) {
        List<Profile> profiles = profileRepository.findAllByLocationGridId(gridId);

        return profiles.stream()
                .filter(Profile::isWeatherNotificationEnabled)
                .map(Profile::getUser)
                .collect(Collectors.toList());
    }

    /**
     * 감지된 날씨 변화(DTO)에 따라 사용자 목록에게 개별 알림을 생성하고 발송합니다.
     *
     * @param usersToNotify 알림을 수신할 사용자 목록
     * @param changes       변화된 날씨 정보가 담긴 DTO
     */
    private void sendNotifications(List<User> usersToNotify, WeatherChangeDto changes) {
        List<UUID> receiverIds = usersToNotify.stream().map(User::getId).toList();

        // [온도 변화]에 대한 알림
        if (changes.tempComparedToDayBefore() != null) {
            double temp = changes.tempComparedToDayBefore();
            String title = "기온 변화 알림 🌡️";
            String message = temp > 0 ?
                    String.format("어제보다 기온이 %.1f도 높아요! 가벼운 옷차림은 어떠세요? ☀️", temp) :
                    String.format("어제보다 기온이 %.1f도 낮아요. 따뜻하게 입으세요! 🧣", Math.abs(temp));

            eventPublisher.publishEvent(new WeatherNotificationEvent(receiverIds, title, message, NotificationLevel.INFO));
        }

        // [습도 변화]에 대한 알림
        if (changes.humidComparedToDayBefore() != null) {
            double humid = changes.humidComparedToDayBefore();
            String title = "습도 변화 알림 💦";
            String message = humid > 0 ?
                    String.format("어제보다 습도가 %.1f%%p 높아요! 불쾌 지수에 유의하세요! 🥹", humid) :
                    String.format("어제보다 습도가 %.1f%%p 낮아요. 즐거운 하루 되세요! ❤️", Math.abs(humid));

            eventPublisher.publishEvent(new WeatherNotificationEvent(receiverIds, title, message, NotificationLevel.INFO));
        }

        // [하늘 상태 변화]에 대한 알림
        if (changes.skyStatus() != null) {
            String title = "하늘 변화 알림 💭";
            String message = switch (changes.skyStatus()) {
                case MOSTLY_CLOUDY -> "어제보다 구름이 약간 많아요. 🌥️";
                case CLOUDY -> "오늘은 날이 흐려요. ☁️";
                default -> "오늘은 날이 맑아요! 오늘 하루는 좋은 사람과 지내는건 어때요? 👩‍👧‍👦";
            };
            eventPublisher.publishEvent(new WeatherNotificationEvent(receiverIds, title, message, NotificationLevel.INFO));
        }

        // [강수 변화]에 대한 알림
        if (changes.precipitation() != null && changes.precipitation() != Precipitation.NONE) {
            String title = "강수 변화 알림 🌧️";
            String message = "곧 비나 눈이 올 수 있으니, 우산을 챙기는 걸 잊지 마세요! ☔";

            eventPublisher.publishEvent(new WeatherNotificationEvent(receiverIds, title, message, NotificationLevel.INFO));
        }

        // [바람 상태 변화]에 대한 알림을 보냅니다.
        if (changes.windAsWord() != null && changes.windAsWord() != WindAsWord.WEAK) {
            String title = "바람 변화 알림 💨";
            String message = "바람이 어제보다 강하게 불고 있어요. 안전에 유의하세요! ⛑️";

            eventPublisher.publishEvent(new WeatherNotificationEvent(receiverIds, title, message, NotificationLevel.INFO));
        }
    }
}
