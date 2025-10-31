package com.samsamotot.otboo.weather.service;

import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.entity.Grid;

import java.util.List;

/**
 * 날씨 수집 서비스 인터페이스
 * 
 * <p>기상청 단기예보 Open API를 호출하여 날씨 정보를 수집하고 저장하는 서비스입니다.
 * 비동기 처리를 통해 대량의 격자 좌표에 대한 날씨 데이터를 효율적으로 수집합니다.</p>
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>기상청 API 호출 및 응답 파싱</li>
 *   <li>날씨 데이터 데이터베이스 저장</li>
 *   <li>비동기 처리로 성능 최적화</li>
 * </ul>
 * 
 * @author HuInDoL
 * @since 1.0
 * @see Grid
 */
public interface WeatherService {

    void updateWeatherForGrid(int x, int y);

    List<WeatherDto> getWeatherList(double longitude, double latitude);
}
