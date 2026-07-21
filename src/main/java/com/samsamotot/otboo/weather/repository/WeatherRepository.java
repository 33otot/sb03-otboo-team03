package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID>, WeatherRepositoryCustom {

    /**
     * 특정 격자의 어제 동일한 발표/예보 시각의 날씨 정보를 조회합니다.
     */
    Optional<Weather> findByGridAndForecastedAtAndForecastAt(Grid grid, Instant forecastedAt, Instant forecastAt);
}


