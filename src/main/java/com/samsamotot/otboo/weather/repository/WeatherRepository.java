package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    void deleteByGridAndForecastAtBefore(Grid grid, Instant threshold);

    List<Weather> findAllByGrid(Grid grid);

    /**
     * 특정 격자(grid)의 데이터 중, 주어진 시작 시간과 종료 시간 사이의 예보를 조회합니다.
     * '전날' 데이터 조회 시 사용됩니다.
     */
    List<Weather> findByGridAndForecastAtBetween(Grid grid, Instant start, Instant end);}
