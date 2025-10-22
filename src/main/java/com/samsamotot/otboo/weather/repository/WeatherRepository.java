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

public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    @Query("SELECT w FROM Weather w JOIN FETCH w.grid WHERE w.grid = :grid")
    List<Weather> findAllByGrid(@Param("grid") Grid grid);

    /**
     * 특정 격자와 예보 시각에 해당하는 가장 최신의 예보를 하나만 조회합니다.
     * @param grid 격자 정보
     * @param forecastAt 예보 대상 시각
     * @return Optional<Weather>
     */
    Optional<Weather> findTopByGridAndForecastAtOrderByForecastedAtDesc(Grid grid, Instant forecastAt);

    /**
     * [삭제 로직 1] 정말 오래된 예보 데이터 삭제 (Feed 참조 없을 시)
     * 특정 격자(Grid)에서 특정 시각(threshold) 이전의 '예보 시각(forecastAt)'을 가진 데이터 중,
     * 어떤 Feed에서도 참조되지 않는 데이터만 삭제
     */
    @Modifying
    @Query("DELETE FROM Weather w WHERE w.grid = :grid AND w.forecastAt < :threshold " +
            "AND NOT EXISTS (SELECT 1 FROM Feed f WHERE f.weather = w)")
    int deleteOldAndUnreferencedWeather(@Param("grid") Grid grid, @Param("threshold") Instant threshold);

    /**
     * [삭제 로직 2] 최신 예보로 대체된 "오래된 발표" 데이터 삭제 (Feed 참조 없을 시)
     * 특정 격자(Grid) 내에서, 동일한 '예보 시각(forecastAt)'에 대해
     * 더 최신 '발표 시각(forecastedAt)'을 가진 데이터가 존재할 경우,
     * 현재 데이터(오래된 발표 시각)가 Feed에서 참조되지 않으면 삭제
     * @param grid 삭제 대상 격자
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM Weather w1 WHERE w1.grid = :grid " + // 특정 그리드의 날씨(w1) 중에서
            // 동일한 grid와 forecastAt을 가지면서, w1보다 더 최신 forecastedAt을 가진 w2가 존재하고,
            "AND EXISTS (SELECT 1 FROM Weather w2 WHERE w2.grid = w1.grid AND w2.forecastAt = w1.forecastAt AND w2.forecastedAt > w1.forecastedAt) " +
            // w1이 어떤 Feed에도 참조되지 않을 때만 삭제
            "AND NOT EXISTS (SELECT 1 FROM Feed f WHERE f.weather = w1)")
    int deleteOutdatedAndUnreferencedWeather(@Param("grid") Grid grid);


    /**
     * 특정 격자의 어제 동일한 발표/예보 시각의 날씨 정보를 조회합니다.
     */
    Optional<Weather> findByGridAndForecastedAtAndForecastAt(Grid grid, Instant forecastedAt, Instant forecastAt);

//    /**
//     * 특정 격자와 정확한 예보된 시각을 기준으로 존재하는 날씨 데이터를 삭제합니다.
//     */
//    @Modifying
//    @Query("DELETE FROM Weather w WHERE w.grid = :grid AND w.forecastAt < :threshold " +
//            "AND NOT EXISTS (SELECT 1 FROM Feed f WHERE f.weather = w)")
//    void deleteByGridAndForecastAtBeforeNotReferencedByFeeds(
//            @Param("grid") Grid grid,
//            @Param("threshold") Instant threshold);

//    @Modifying
//    @Query("DELETE FROM Weather w WHERE w.grid = :grid AND w.forecastAt < :threshold " +
//            "AND NOT EXISTS (SELECT 1 FROM Feed f WHERE f.weather = w)")
//    void deleteByGridAndForecastAtBefore(
//            @Param("grid") Grid grid,
//            @Param("threshold") Instant threshold);
}


