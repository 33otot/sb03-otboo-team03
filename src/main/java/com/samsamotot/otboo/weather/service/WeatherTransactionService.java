package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
     * 특정 격자(Grid)의 날씨 예보를 업데이트하고, 전날 대비 값을 계산하여 저장합니다.
     * @param grid 날씨를 업데이트할 격자
     * @param newWeatherList API로부터 새로 수신한 예보 목록
     */
    public void updateWeather(Grid grid, List<Weather> newWeatherList) {
        // 새 데이터가 비어있으면 기존 데이터를 보존합니다.
        if (newWeatherList == null || newWeatherList.isEmpty()) {
            return;
        }

        newWeatherList.sort(Comparator.comparing(Weather::getForecastAt));

        // 1. [데이터 준비] comparedToDayBefore 필드에 필요한 모든 날씨 데이터 Map
        Map<Instant, Weather> comparisonWeather = new HashMap<>();

        // 2. [DB 조회] FETCH 당일의 전날 데이터만 DB에서 조회
        fetchAndPoolYesterdayWeather(grid, newWeatherList.get(0), comparisonWeather);

        // 3. [계산] 정렬된 예보 리스트를 순회하며 전날 대비 값 계산하고 다음 날 비교 위한 데이터 준비
        processAndPoolNewWeathers(newWeatherList, comparisonWeather);
        fillInMissingDailyTemperatures(grid, newWeatherList);

        // 4. [DB 정리] 오래된 데이터와 중복될 수 있는 기존 데이터 삭제하여 DB 최신화
        cleanupDatabase(grid, newWeatherList.get(0));

        // 5. [저장] 전날 대비 값이 계산된 새로운 날씨 데이터 저장
        weatherRepository.saveAll(newWeatherList);
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
                // computeIfAbsent: 캐시에 값이 없으면 DB 조회 함수를 실행
                Double maxTemp = maxTempCache.computeIfAbsent(date, d -> weatherDailyValueProvider.findDailyTemperatureValue(grid, d, true));
                if (maxTemp != null) {
                    weather.setTemperatureMax(maxTemp);
                }
            }

            if (weather.getTemperatureMin() == null) {
                Double minTemp = minTempCache.computeIfAbsent(date, d -> weatherDailyValueProvider.findDailyTemperatureValue(grid, d, false));
                if (minTemp != null) {
                    weather.setTemperatureMin(minTemp);
                }
            }
        }
        log.info(SERVICE_NAME + "최고/최저 기온 값 이어주기 완료. 처리된 날짜 수: {}", maxTempCache.size() + minTempCache.size());
    }

    /**
     * 새로운 데이터를 저장하기 전에 DB를 정리합니다.
     */
    private void cleanupDatabase(Grid grid, Weather firstDayWeather) {
        Instant baseDateTime = firstDayWeather.getForecastedAt();
        weatherRepository.deleteByGridAndForecastedAt(grid, baseDateTime);

        Instant deleteThreshold = Instant.now().minus(3, ChronoUnit.DAYS);
        weatherRepository.deleteByGridAndForecastAtBefore(grid, deleteThreshold);
    }
}