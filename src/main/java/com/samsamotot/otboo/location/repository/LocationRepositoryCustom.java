package com.samsamotot.otboo.location.repository;

import com.samsamotot.otboo.location.entity.Location;

import java.util.List;
import java.util.Optional;

public interface LocationRepositoryCustom {
    /**
     * 경도와 위도를 조건으로 하여 위치 정보를 조회합니다.
     *
     * @param longitude 경도
     * @param latitude 위도
     * @return 일치하는 위치 정보 (Optional)
     */
    Optional<Location> findByLongitudeAndLatitude(double longitude, double latitude);

    /**
     * 격자 정보(Grid)를 Fetch Join 하여 모든 위치 정보를 조회합니다.
     *
     * @return 격자 정보가 포함된 모든 위치 목록
     */
    List<Location> findAllWithGrid();
}
