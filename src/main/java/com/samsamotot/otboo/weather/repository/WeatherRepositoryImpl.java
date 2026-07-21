package com.samsamotot.otboo.weather.repository;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.feed.entity.QFeed;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.QGrid;
import com.samsamotot.otboo.weather.entity.QWeather;
import com.samsamotot.otboo.weather.entity.Weather;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WeatherRepositoryImpl implements WeatherRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QWeather qWeather = QWeather.weather;
    private final QGrid qGrid = QGrid.grid;
    private final QFeed qFeed = QFeed.feed;

    /**
     * 특정 격자에 속하는 모든 예보 정보를 조회합니다.
     * 격자 정보를 Fetch Join 하여 한 번에 가져옵니다.
     *
     * @param grid 격자 정보
     * @return 특정 격자에 속한 예보 목록
     */
    @Override
    public List<Weather> findAllByGrid(Grid grid) {
        return queryFactory
                .selectFrom(qWeather)
                .join(qWeather.grid, qGrid).fetchJoin()
                .where(qWeather.grid.eq(grid))
                .fetch();
    }

    /**
     * 특정 격자와 예보 대상 시각에 해당하는 가장 최신의 발표 데이터를 하나만 조회합니다.
     * 발표 시각(forecastedAt) 기준 내림차순 정렬 후 최상위 1건을 반환합니다.
     *
     * @param grid 격자 정보
     * @param forecastAt 예보 대상 시각
     * @return 조건에 부합하는 가장 최신의 예보 데이터 (Optional)
     */
    @Override
    public Optional<Weather> findTopByGridAndForecastAtOrderByForecastedAtDesc(Grid grid, Instant forecastAt) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(qWeather)
                        .where(qWeather.grid.eq(grid)
                                .and(qWeather.forecastAt.eq(forecastAt)))
                        .orderBy(qWeather.forecastedAt.desc())
                        .limit(1)
                        .fetchOne()
        );
    }

    /**
     * 특정 격자에서 지정된 기준 시각 이전의 예보 데이터 중,
     * 어떤 피드(Feed)에서도 참조되지 않는 오래된 데이터를 삭제합니다.
     *
     * @param grid 격자 정보
     * @param threshold 삭제 대상 기준 예보 시각
     * @return 삭제된 레코드 수
     */
    @Override
    public int deleteOldAndUnreferencedWeather(Grid grid, Instant threshold) {
        long deletedCount = queryFactory
                .delete(qWeather)
                .where(qWeather.grid.eq(grid)
                        .and(qWeather.forecastAt.before(threshold))
                        .and(JPAExpressions
                                .selectOne()
                                .from(qFeed)
                                .where(qFeed.weather.eq(qWeather))
                                .exists()
                                .not()
                        ))
                .execute();
        return (int) deletedCount;
    }

    /**
     * 특정 격자 내에서, 동일한 예보 시각에 대해 더 최신의 발표 시각을 가진 데이터가 존재하고,
     * 피드(Feed)에서 참조하지 않는 오래된 발표 데이터를 일괄 삭제합니다.
     *
     * @param grid 격자 정보
     * @return 삭제된 레코드 수
     */
    @Override
    public int deleteOutdatedAndUnreferencedWeather(Grid grid) {
        QWeather w2 = new QWeather("w2");
        long deletedCount = queryFactory
                .delete(qWeather)
                .where(qWeather.grid.eq(grid)
                        .and(JPAExpressions
                                .selectOne()
                                .from(w2)
                                .where(w2.grid.eq(qWeather.grid)
                                        .and(w2.forecastAt.eq(qWeather.forecastAt))
                                        .and(w2.forecastedAt.gt(qWeather.forecastedAt)))
                                .exists())
                        .and(JPAExpressions
                                .selectOne()
                                .from(qFeed)
                                .where(qFeed.weather.eq(qWeather))
                                .exists()
                                .not()
                        ))
                .execute();
        return (int) deletedCount;
    }
}
