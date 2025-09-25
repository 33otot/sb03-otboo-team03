package com.samsamotot.otboo.location.service;

import com.samsamotot.otboo.location.entity.WeatherAPILocation;

/**
 * 위치 정보 조회 서비스 인터페이스
 * 
 * <p>좌표 기반으로 위치 정보를 조회하고 관리하는 서비스입니다.
 * 카카오 로컬 API를 통해 주소 정보를 조회하고, 기상청 격자 좌표로 변환합니다.</p>
 * 
 * @author HuInDoL
 * @since 1.0
 * @see WeatherAPILocation
 */
public interface LocationService {

    /**
     * 좌표를 기반으로 현재 위치 정보를 조회합니다.
     * 
     * <p>기존 위치 정보가 있으면 캐시된 데이터를 반환하고,
     * 없거나 오래된 경우 카카오 로컬 API를 호출하여 최신 정보를 조회합니다.</p>
     * 
     * @param longitude 경도 (WGS84 좌표계)
     * @param latitude  위도 (WGS84 좌표계)
     * @return WeatherAPILocation 위치 정보 객체
     */
    WeatherAPILocation getCurrentLocation(double longitude, double latitude);
}
