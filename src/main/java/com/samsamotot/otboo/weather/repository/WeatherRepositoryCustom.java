package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WeatherRepositoryCustom {
    /**
     * 특정 격자에 속하는 모든 예보 정보를 조회합니다.
     * 격자 정보를 Fetch Join 하여 한 번에 가져옵니다.
     *
     * @param grid 격자 정보
     * @return 특정 격자에 속한 예보 목록
     */
    List<Weather> findAllByGrid(Grid grid);

    /**
     * 특정 격자와 예보 대상 시각에 해당하는 가장 최신의 발표 데이터를 하나만 조회합니다.
     * 발표 시각(forecastedAt) 기준 내림차순 정렬 후 최상위 1건을 반환합니다.
     *
     * @param grid 격자 정보
     * @param forecastAt 예보 대상 시각
     * @return 조건에 부합하는 가장 최신의 예보 데이터 (Optional)
     */
    Optional<Weather> findTopByGridAndForecastAtOrderByForecastedAtDesc(Grid grid, Instant forecastAt);

    /**
     * 특정 격자에서 지정된 기준 시각 이전의 예보 데이터 중,
     * 어떤 피드(Feed)에서도 참조되지 않는 오래된 데이터를 삭제합니다.
     *
     * @param grid 격자 정보
     * @param threshold 삭제 대상 기준 예보 시각
     * @return 삭제된 레코드 수
     */
    int deleteOldAndUnreferencedWeather(Grid grid, Instant threshold);

    /**
     * 특정 격자 내에서, 동일한 예보 시각에 대해 더 최신의 발표 시각을 가진 데이터가 존재하고,
     * 피드(Feed)에서 참조하지 않는 오래된 발표 데이터를 일괄 삭제합니다.
     *
     * @param grid 격자 정보
     * @return 삭제된 레코드 수
     */
    int deleteOutdatedAndUnreferencedWeather(Grid grid);
}
