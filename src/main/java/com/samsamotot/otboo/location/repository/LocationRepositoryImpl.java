package com.samsamotot.otboo.location.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.entity.QLocation;
import com.samsamotot.otboo.weather.entity.QGrid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LocationRepositoryImpl implements LocationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QLocation qLocation = QLocation.location;
    private final QGrid qGrid = QGrid.grid;

    /**
     * 경도와 위도를 조건으로 하여 위치 정보를 조회합니다.
     *
     * @param longitude 경도
     * @param latitude 위도
     * @return 일치하는 위치 정보 (Optional)
     */
    @Override
    public Optional<Location> findByLongitudeAndLatitude(double longitude, double latitude) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(qLocation)
                        .where(qLocation.longitude.eq(longitude)
                                .and(qLocation.latitude.eq(latitude)))
                        .fetchOne()
        );
    }

    /**
     * 격자 정보(Grid)를 Fetch Join 하여 모든 위치 정보를 조회합니다.
     *
     * @return 격자 정보가 포함된 모든 위치 목록
     */
    @Override
    public List<Location> findAllWithGrid() {
        return queryFactory
                .selectFrom(qLocation)
                .join(qLocation.grid, qGrid).fetchJoin()
                .fetch();
    }
}
