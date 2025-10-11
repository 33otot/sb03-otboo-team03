package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    void deleteByGridAndForecastAtBefore(Grid grid, Instant threshold);

    @Query("SELECT w FROM Weather w JOIN FETCH w.grid WHERE w.grid = :grid")
    List<Weather> findAllByGrid(@Param("grid") Grid grid);

    /**
     * 특정 격자와 예보 시각에 해당하는 가장 최신의 예보를 하나만 조회합니다.
     * @param grid 격자 정보
     * @param forecastAt 예보 대상 시각
     * @return Optional<Weather>
     */
    /**
     * 특정 격자와 예보 시각에 해당하는 가장 최신의 예보를 하나만 조회합니다.
     * @param grid 격자 정보
     * @param forecastAt 예보 대상 시각
     * @return Optional<Weather>
     */
    Optional<Weather> findTopByGridAndForecastAtOrderByForecastedAtDesc(Grid grid, Instant forecastAt);

    /**
     * 특정 격자와 정확한 예보된 시각을 기준으로 존재하는 날씨 데이터를 삭제합니다.
     */
    void deleteByGridAndForecastedAt(Grid grid, Instant forecastIssuedAt);

    /**
      * 특정 격자의 어제 동일한 발표/예보 시각의 날씨 정보를 조회합니다.
      * */
    Optional<Weather> findByGridAndForecastedAtAndForecastAt(Grid grid, Instant forecastedAt, Instant forecastAt);
}


