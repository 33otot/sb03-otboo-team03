package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WeatherTransactionService {

    private static final String SERVICE_NAME = "[WeatherTransactionService] ";

    private final WeatherRepository weatherRepository;
    private final WeatherDailyValueProvider weatherDailyValueProvider;

    /**
     * 특정 격자(Grid)의 날씨 예보를 업데이트하고, 전날 대비 값을 계산하여 저장합니다. (Upsert 방식 적용)
     * @param grid 날씨를 업데이트할 격자
     * @param newWeatherList API로부터 새로 수신한 예보 목록
     */
    public void updateWeather(Grid grid, List<Weather> newWeatherList) {
        if (newWeatherList == null || newWeatherList.isEmpty()) {
            log.warn(SERVICE_NAME + "새 날씨 데이터 목록이 비어있어 업데이트를 건너뜁니다. Grid: {}", grid);
            return;
        }

        newWeatherList.sort(Comparator.comparing(Weather::getForecastAt));

        // 1. [데이터 준비] 전날 비교를 위한 데이터 풀
        Map<Instant, Weather> comparisonWeather = new HashMap<>();
        fetchAndPoolYesterdayWeather(grid, newWeatherList.get(0), comparisonWeather);

        // 2. [계산] 전날 대비 값 계산 및 최고/최저 기온 보간
        processAndPoolNewWeathers(newWeatherList, comparisonWeather);
        fillInMissingDailyTemperatures(grid, newWeatherList);

        // 3. [저장 - Upsert] 새 데이터를 순회하며 있으면 수정, 없으면 삽입 (saveAll 대신 사용!)
        saveOrUpdateWeathers(newWeatherList);

        // 4. [DB 정리] 오래된 데이터 및 구식 발표 데이터 삭제
        cleanupDatabase(grid);

        log.info(SERVICE_NAME + "날씨 정보 업데이트 및 정리 완료. Grid ID: {}", grid.getId());
    }

    /**
     * Upsert 로직: 엔티티를 조회하여 있으면 변경 감지(Dirty Checking)로 UPDATE, 없으면 INSERT.
     * @param newWeatherList API로부터 받아 처리된 날씨 데이터 목록
     */
    private void saveOrUpdateWeathers(List<Weather> newWeatherList) {
        AtomicInteger insertedCount = new AtomicInteger(0);
        AtomicInteger updatedCount = new AtomicInteger(0);
        for (Weather newWeather : newWeatherList) {
            // UNIQUE 제약조건 컬럼(grid, forecastAt, forecastedAt)으로 기존 데이터 조회
            Optional<Weather> existingWeatherOpt = weatherRepository.findByGridAndForecastedAtAndForecastAt(
                    newWeather.getGrid(),
                    newWeather.getForecastedAt(),
                    newWeather.getForecastAt()
            );

            try {
                if (existingWeatherOpt.isPresent()) {
                    // 데이터가 있으면, 기존 엔티티의 값을 업데이트
                    updateWeatherEntity(existingWeatherOpt.get(), newWeather);
                    updatedCount.incrementAndGet();
                } else {
                    // 데이터가 없으면, 새로 저장
                    weatherRepository.save(newWeather);
                    insertedCount.incrementAndGet();
                }
            } catch (DataIntegrityViolationException ex) {
                // 동시 삽입 경합으로 UNIQUE 제약조건 위반 시, 재조회 후 UPDATE로 전환
                log.warn(SERVICE_NAME + "데이터 삽입 경합 발생. 업데이트로 전환합니다. Weather: {}", newWeather);
                weatherRepository.findByGridAndForecastedAtAndForecastAt(
                                newWeather.getGrid(), newWeather.getForecastedAt(), newWeather.getForecastAt())
                        .ifPresentOrElse(
                                retryWeather -> {
                                    updateWeatherEntity(retryWeather, newWeather);
                                    updatedCount.incrementAndGet();
                                },
                                () -> {
                                    // 재조회 실패 시, 예측 못한 다른 원인이므로 예외를 다시 던짐
                                    throw ex;
                                }
                        );
            }
        }
        log.info(SERVICE_NAME + "날씨 데이터 Upsert 완료: 삽입={}, 업데이트={}. Grid ID: {}", 
                insertedCount, updatedCount, newWeatherList.isEmpty() ? "N/A" : newWeatherList.get(0).getGrid().getId());
    }

    /**
     * 기존 날씨 엔티티(existing)를 새로운 날씨 데이터(newData)로 업데이트합니다.
     * @param existing DB에 이미 존재하는 엔티티
     * @param newData API로부터 받은 새로운 데이터
     */
    private void updateWeatherEntity(Weather existing, Weather newData) {
        existing.setTemperatureCurrent(newData.getTemperatureCurrent());
        existing.setTemperatureComparedToDayBefore(newData.getTemperatureComparedToDayBefore());
        existing.setTemperatureMin(newData.getTemperatureMin());
        existing.setTemperatureMax(newData.getTemperatureMax());
        existing.setSkyStatus(newData.getSkyStatus());
        existing.setPrecipitationType(newData.getPrecipitationType());
        existing.setPrecipitationAmount(newData.getPrecipitationAmount());
        existing.setPrecipitationProbability(newData.getPrecipitationProbability());
        existing.setHumidityCurrent(newData.getHumidityCurrent());
        existing.setHumidityComparedToDayBefore(newData.getHumidityComparedToDayBefore());
        existing.setWindSpeed(newData.getWindSpeed());
        existing.setWindAsWord(newData.getWindAsWord());
    }
    
    /**
     * 새로운 예보 목록의 첫날과 비교하기 위한 바로 전날의 데이터를 DB에서 조회하여 데이터 풀에 추가합니다.
     */
    private void fetchAndPoolYesterdayWeather(Grid grid, Weather firstDayWeather, Map<Instant, Weather> comparisonWeather) {
        Instant yesterday = firstDayWeather.getForecastAt().minus(1, ChronoUnit.DAYS);

        Optional<Weather> yesterdayWeather = weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, yesterday);
        yesterdayWeather.ifPresent(weather -> comparisonWeather.put(weather.getForecastAt(), weather));
    }

    /**
     * 새로운 예보 목록을 순회하며 전날 대비 값을 계산하고, 다음 날의 계산을 위해 현재 데이터를 풀에 추가합니다.
     */
    private void processAndPoolNewWeathers(List<Weather> newWeatherList, Map<Instant, Weather> comparisonWeather) {
        for (Weather currentDayWeather : newWeatherList) {
            Instant yesterday = currentDayWeather.getForecastAt().minus(1, ChronoUnit.DAYS);
            Weather yesterdayWeather = comparisonWeather.get(yesterday);

            if (yesterdayWeather != null) {
                if (currentDayWeather.getTemperatureCurrent() != null && yesterdayWeather.getTemperatureCurrent() != null) {
                    double tempComparedToDayBefore = currentDayWeather.getTemperatureCurrent() - yesterdayWeather.getTemperatureCurrent();
                    currentDayWeather.setTemperatureComparedToDayBefore(tempComparedToDayBefore);
                }
                if (currentDayWeather.getHumidityCurrent() != null && yesterdayWeather.getHumidityCurrent() != null) {
                    double humidComparedToDayBefore = currentDayWeather.getHumidityCurrent() - yesterdayWeather.getHumidityCurrent();
                    currentDayWeather.setHumidityComparedToDayBefore(humidComparedToDayBefore);
                }
            }

            comparisonWeather.put(currentDayWeather.getForecastAt(), currentDayWeather);
        }
    }

    /**
     * 날짜별 최고/최저 기온을 채워주는 통합 메소드.
     * "선 스캔, 후 채우기" 전략으로 정확도를 높입니다.
     */
    private void fillInMissingDailyTemperatures(Grid grid, List<Weather> newWeatherList) {
        Map<LocalDate, Double> maxTempCache = new HashMap<>();
        Map<LocalDate, Double> minTempCache = new HashMap<>();

        // 1단계: "선 스캔" - API로 받은 데이터에서 최고/최저 기온을 미리 추출하여 캐시에 저장
        for (Weather weather : newWeatherList) {
            LocalDate date = weather.getForecastAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
            if (weather.getTemperatureMax() != null) {
                maxTempCache.put(date, weather.getTemperatureMax());
            }
            if (weather.getTemperatureMin() != null) {
                minTempCache.put(date, weather.getTemperatureMin());
            }
        }

        // 2단계: "후 채우기" - null 값을 캐시와 DB를 통해 채워넣기
        for (Weather weather : newWeatherList) {
            LocalDate date = weather.getForecastAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();

            if (weather.getTemperatureMax() == null) {
                if (!maxTempCache.containsKey(date)) {
                    Double maxTemp = weatherDailyValueProvider.findDailyTemperatureValue(grid, date, true);
                    maxTempCache.put(date, maxTemp);
                }
                Double maxTempFromCache = maxTempCache.get(date);
                if (maxTempFromCache != null) {
                    weather.setTemperatureMax(maxTempFromCache);
                }
            }

            if (weather.getTemperatureMin() == null) {
                if (!minTempCache.containsKey(date)) {
                    Double minTemp = weatherDailyValueProvider.findDailyTemperatureValue(grid, date, false);
                    minTempCache.put(date, minTemp);
                }
                Double minTempFromCache = minTempCache.get(date);
                if (minTempFromCache != null) {
                    weather.setTemperatureMin(minTempFromCache);
                }
            }
        }
        log.info(SERVICE_NAME + "최고/최저 기온 값 이어주기 완료. 처리된 날짜 수: {}", maxTempCache.size() + minTempCache.size());
    }

    /**
     * DB를 정리하는 로직을 2단계로 분리하여 안전하게 실행
     * 1. 정말 오래된 데이터 (3일 이상 지난 forecast_at) 중 참조 안 된 것 삭제
     * 2. 최신 예보로 대체된 오래된 발표 데이터 중 참조 안 된 것 삭제
     */
    private void cleanupDatabase(Grid grid) {
        // 단계 1: 정말 오래된 예보 데이터 삭제 (Feed 참조 없을 시)
        Instant deleteThreshold = Instant.now().minus(3, ChronoUnit.DAYS);
        // 새로 만든 Repository 메서드를 호출합니다.
        int deletedOldCount = weatherRepository.deleteOldAndUnreferencedWeather(grid, deleteThreshold);
        if (deletedOldCount > 0) {
            log.info(SERVICE_NAME + "참조되지 않는 오래된 날씨(3일+) 데이터 {}건 삭제 완료. Grid ID: {}", deletedOldCount, grid.getId());
        }

        // 단계 2: 최신 예보로 대체된 "오래된 발표" 데이터 삭제 (Feed 참조 없을 시)
        // 새로 만든 Repository 메서드를 호출합니다.
        int deletedOutdatedCount = weatherRepository.deleteOutdatedAndUnreferencedWeather(grid);
        if (deletedOutdatedCount > 0) {
            log.info(SERVICE_NAME + "참조되지 않는 오래된 발표 시각 날씨 데이터 {}건 삭제 완료. Grid ID: {}", deletedOutdatedCount, grid.getId());
        }

        if (deletedOldCount == 0 && deletedOutdatedCount == 0) {
            log.info(SERVICE_NAME + "삭제할 오래된 날씨 데이터 없음 (또는 모두 참조 중). Grid ID: {}", grid.getId());
        }
    }
}