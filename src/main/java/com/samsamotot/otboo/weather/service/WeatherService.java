package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.entity.Grid;

import java.util.concurrent.CompletableFuture;

/**
 * 날씨 수집 서비스 인터페이스.
 * <p>
 * - 기상청 단기예보 Open API를 호출해 응답을 파싱하고, 도메인 모델로 저장하는 진입점을 정의합니다.
 * </p>
 *
 * @author HuInDoL
 */
public interface WeatherService {

    /**
     * 특정 격자 좌표에 대한 최신 날씨 정보를 가져와 DB를 업데이트합니다.
     * 이 메소드는 하나의 트랜잭션으로 동작해야 합니다.
     * @param grid 날씨 업데이트할 대한민국 위치 정보 격자
     */
    CompletableFuture<Void> updateWeatherDataForGrid(Grid grid);
}
