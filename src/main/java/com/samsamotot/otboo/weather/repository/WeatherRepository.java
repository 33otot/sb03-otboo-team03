package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    void deleteByGridAndForecastAtBefore(Grid grid, Instant threshold);

    @Query("SELECT w FROM Weather w JOIN FETCH w.grid WHERE w.grid = :grid")
    List<Weather> findAllByGrid(@Param("grid") Grid grid);

    /**
     * 특정 격자(grid)의 데이터 중, 주어진 시작 시간과 종료 시간 사이의 예보를 조회합니다.
     * '전날' 데이터 조회 시 사용됩니다.
     */
    List<Weather> findByGridAndForecastAtBetween(Grid grid, Instant start, Instant end);

    /**
     * 특정 격자와 정확한 예보 시각으로 날씨 데이터를 조회합니다.
     */
    Optional<Weather> findByGridAndForecastAt(Grid grid, Instant forecastAt);

    void deleteByGridAndForecastedAt(Grid grid, Instant forecastIssuedAt);
}


