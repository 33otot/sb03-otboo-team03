package com.samsamotot.otboo.recommendation.service;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.entity.ClothesType;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.recommendation.dto.RecommendationContextDto;
import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추천(Recommendation) 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationServiceImpl implements RecommendationService {

    private static final double SENSITIVITY_BASE = 2.5;     // 기준값 상수
    private static final double CORRECTION_FACTOR = 1.0;    // 보정 강도

    private static final String SEVICE = "[RecommendationServiceImpl] ";

    private final ProfileRepository profileRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
    private final ItemSelectorEngine itemSelectorEngine;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 사용자와 날씨 정보를 기반으로 의상 추천을 수행합니다.
     *
     * <p>주요 처리 과정:
     * <ul>
     *   <li>사용자 프로필(온도 민감도 등) 및 날씨 정보(기온, 풍속, 습도, 강수 등) 조회</li>
     *   <li>사용자 온도 민감도(0~5, 낮을수록 추위 민감, 높을수록 더위 민감)를 반영하여 체감온도 보정
     *     <br>보정 공식: <code>보정값 = (민감도 - 2.5) * k</code> (k=1.0, 최대 ±2.5℃)</li>
     *   <li>계절별 체감온도 산출:
     *     <ul>
     *       <li>겨울(10℃ 이하, 풍속 4.8km/h 이상): 기상청 Wind Chill 공식 적용<br>
     *         출처: <a href="https://data.kma.go.kr/climate/windChill/selectWindChillChart.do">기상청 체감온도 안내</a>
     *       </li>
     *       <li>여름(27℃ 이상 & 습도 40% 이상): 기상청 체감온도(2022.6.2. 개정, 습구온도 기반)<br>
     *         출처: <a href="https://data.kma.go.kr/climate/windChill/selectWindChillChart.do">기상청 체감온도 안내</a>
     *       </li>
     *       <li>그 외: Steadman Apparent Temperature 공식 적용</li>
     *     </ul>
     *   </li>
     *   <li>강수(비/눈) 여부, 월 정보, Redis 기반 추천 이력 등 컨텍스트 생성</li>
     *   <li>추천 엔진 호출 및 결과 반환</li>
     * </ul>
     *
     * @param userId 추천 대상 사용자 ID
     * @param weatherId 추천 기준이 되는 날씨 정보 ID
     * @return 추천 결과 DTO
     * @throws OtbooException 사용자 프로필 또는 날씨 정보가 없을 경우 발생
     */
    @Override
    public RecommendationDto recommendClothes(UUID userId, UUID weatherId) {

        // 사용자 프로필 조회
        Profile userProfile = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new OtbooException(ErrorCode.PROFILE_NOT_FOUND));

        // 날씨 정보 조회
        Weather weather = weatherRepository.findById(weatherId)
            .orElseThrow(() -> new OtbooException(ErrorCode.WEATHER_NOT_FOUND));

        // 사용자의 의상 목록 조회
        List<Clothes> clothesList = clothesRepository.findAllByOwnerId(userId);

        if (clothesList.isEmpty()) {
            log.debug(SEVICE + "사용자 의상 목록이 비어 있습니다. userId: {}", userId);
            throw new OtbooException(ErrorCode.CLOTHES_NOT_FOUND);
        }

        // 사용자 온도 민감도(0~5) 및 보정값 계산
        double sensitivity = clamp(userProfile.getTemperatureSensitivity(), 0.0, 5.0);
        double correction = clamp((sensitivity - SENSITIVITY_BASE) * CORRECTION_FACTOR, -2.5, 2.5);

        // 계절별 체감온도 계산 및 민감도 보정
        double adjustedTemperature = calculateFeelsLike(
            weather.getTemperatureCurrent(),
            weather.getWindSpeed(),
            weather.getHumidityCurrent()
        ) + correction;

        // 강수(비/눈) 여부 확인
        boolean isRainingOrSnowing =  weather.getPrecipitationType() != Precipitation.NONE;

        // 현재 월 정보
        Month month = LocalDate.now().getMonth();

        // Redis에서 rollCounter 조회 (추천 횟수)
        long rollCounter = Optional.ofNullable((Long) redisTemplate.opsForValue().get("rollCounter:" + userId))
            .orElse(0L);

        // Redis에서 cooldownIdMap 조회 및 변환 (의상 타입별 쿨타임 관리)
        Map<ClothesType, UUID> cooldownIdMap = getCooldownIdMap(userId);

        // 추천 컨텍스트 DTO 생성
        RecommendationContextDto context = RecommendationContextDto.builder()
            .adjustedTemperature(adjustedTemperature)
            .isRainingOrSnowing(isRainingOrSnowing)
            .currentMonth(month)
            .build();

        // 추천 엔진 호출 및 결과 반환
        List<OotdDto> recommendResult = itemSelectorEngine.createRecommendation(clothesList, context, rollCounter, cooldownIdMap);

        RecommendationDto result = RecommendationDto.builder()
            .userId(userId)
            .weatherId(weatherId)
            .clothes(recommendResult)
            .build();

        // 추천 횟수 증가 후 Redis에 저장
        redisTemplate.opsForValue().set("rollCounter:" + userId, rollCounter + 1);

        return result;
    }

    /**
     * Redis에서 의상 타입별 쿨타임 ID 맵을 조회하고 변환합니다.
     */
    private Map<ClothesType, UUID> getCooldownIdMap(UUID userId) {
        Map<Object, Object> redisMap = redisTemplate.opsForHash().entries("cooldownIdMap:" + userId);
        Map<ClothesType, UUID> cooldownIdMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : redisMap.entrySet()) {
            ClothesType type = null;
            UUID value = null;
            if (entry.getKey() instanceof ClothesType) {
                type = (ClothesType) entry.getKey();
            } else if (entry.getKey() instanceof String) {
                try {
                    type = ClothesType.valueOf((String) entry.getKey());
                } catch (IllegalArgumentException e) {
                    log.warn(SEVICE + "Redis에서 조회한 의상 타입이 유효하지 않습니다: {}", entry.getKey());
                    continue;
                }
            }

            if (entry.getValue() instanceof UUID) {
                value = (UUID) entry.getValue();
            } else if (entry.getValue() instanceof String) {
                try {
                    value = UUID.fromString((String) entry.getValue());
                } catch (IllegalArgumentException e) {
                    log.warn(SEVICE + "Redis에서 조회한 쿨타임 ID가 유효하지 않습니다: {}", entry.getValue());
                    continue;
                }
            }
        }
        return cooldownIdMap;
    }

    /**
     * 온도, 풍속, 습도를 기반으로 계절별 체감온도를 계산합니다.
     * 겨울/여름/그 외 공식 적용 (기상청 및 Steadman)
     */
    private static double calculateFeelsLike(double temperature, double windSpeed, double humidity) {

        final double KMH_THRESHOLD = 4.8;
        final double WINTER_MAX_C = 10.0;
        final double SUMMER_MIN_C = 27.0;
        final double SUMMER_MIN_RH = 40.0;

        final double T = temperature;
        final double RH = clamp(humidity, 0.0, 100.0);
        final double WS = Math.max(0.0, windSpeed); // 음수 방지

        // 겨울: Wind Chill
        if (T <= WINTER_MAX_C) {
            double vKmh = WS * 3.6; // m/s → km/h
            if (vKmh >= KMH_THRESHOLD) {
                double vPow = Math.pow(vKmh, 0.16);
                double wc = 13.12 + 0.6215 * T - 11.37 * vPow + 0.3965 * vPow * T;
                return round1(wc);
            }
            return round1(T);
        }

        // 여름: KMA(습구온도 기반)
        if (T >= SUMMER_MIN_C && RH >= SUMMER_MIN_RH) {
            double Tw = wetBulbTempStull(T, RH);
            double feelsLike = -0.2442
                + 0.55399 * Tw
                + 0.45535 * T
                - 0.0022 * Tw * Tw
                + 0.00278 * Tw * T
                + 3.0;
            return round1(feelsLike);
        }

        // 그 외: Steadman Apparent Temperature (WS: m/s)
        double e = (RH / 100.0) * 6.105 * Math.exp((17.27 * T) / (237.7 + T));
        double at = T + 0.33 * e - 0.70 * WS - 4.00;
        return round1(at);
    }

    /**
     * Stull(2011) 근사식으로 습구온도 계산
     */
    private static double wetBulbTempStull(double T, double RH) {
        double rh = clamp(RH, 0.0, 100.0);
        return T * Math.atan(0.151977 * Math.sqrt(rh + 8.313659))
            + Math.atan(T + rh)
            - Math.atan(rh - 1.67633)
            + 0.00391838 * Math.pow(rh, 1.5) * Math.atan(0.023101 * rh)
            - 4.686035;
    }

    /** 소수점 첫째 자리 반올림 */
    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** 값의 범위 보정 */
    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
