package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class WeatherTransactionService {
    
    private final WeatherRepository weatherRepository;

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
     * 새로운 데이터를 저장하기 전에 DB를 정리합니다.
     */
    private void cleanupDatabase(Grid grid, Weather firstDayWeather) {
        Instant baseDateTime = firstDayWeather.getForecastedAt();
        weatherRepository.deleteByGridAndForecastedAt(grid, baseDateTime);

        Instant deleteThreshold = Instant.now().minus(3, ChronoUnit.DAYS);
        weatherRepository.deleteByGridAndForecastAtBefore(grid, deleteThreshold);
    }
}