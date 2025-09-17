package com.samsamotot.otboo.location.service.impl;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.location.client.KakaoApiClient;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.entity.WeatherAPILocation;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.location.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 위치 정보를 관리하는 서비스 구현 클래스
 * 
 * <p>위치 정보 조회, 저장, 카카오 API를 통한 주소 변환 기능을 제공합니다.
 * 좌표(경도, 위도)를 기반으로 행정구역 정보를 조회하고 관리합니다.</p>
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>좌표 기반 위치 정보 조회</li>
 *   <li>카카오 로컬 API를 통한 주소 변환</li>
 *   <li>위치 정보 데이터베이스 저장</li>
 *   <li>행정구역 정보 우선순위 처리</li>
 * </ul>
 * 
 * @author HuInDoL
 * @since 1.0
 * @see LocationService
 * @see KakaoApiClient
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final String SERVICE = "[LocationServiceImpl] ";

    private final LocationRepository locationRepository;
    private final KakaoApiClient kakaoApiClient;

    /**
     * 좌표를 기반으로 현재 위치 정보를 조회합니다.
     * 
     * <p>처리 과정:</p>
     * <ol>
     *   <li>기존 위치 정보가 있는지 데이터베이스에서 조회</li>
     *   <li>기존 위치가 있으면 해당 정보 반환</li>
     *   <li>기존 위치가 없으면 새 위치 생성 및 저장</li>
     *   <li>카카오 로컬 API를 호출하여 좌표를 행정구역 정보로 변환</li>
     *   <li>변환된 주소 정보를 Location 엔티티에 저장</li>
     * </ol>
     * 
     * <p>카카오 API 호출 시 주소 우선순위:</p>
     * <ul>
     *   <li>1순위: 행정동 (RegionType = "H")</li>
     *   <li>2순위: 법정동 (RegionType = "B")</li>
     * </ul>
     * 
     * <p>카카오 API 호출 실패 시 기본값("UNKNOWN")으로 설정됩니다.</p>
     * 
     * @param longitude 경도 (WGS84 좌표계)
     * @param latitude  위도 (WGS84 좌표계)
     * @return WeatherAPILocation 위치 정보 객체
     * @throws OtbooException 카카오 API 키가 설정되지 않은 경우
     * 
     * @since 1.0
     */
    @Override
    @Transactional
    public WeatherAPILocation getCurrentLocation(double longitude, double latitude) {
        log.info(SERVICE + "위치 조회 시작: longitude={}, latitude={}", longitude, latitude);

        // 1. 기존 위치가 있는지 확인 (위도/경도)
        Location existingLocation = locationRepository.findByLongitudeAndLatitude(longitude, latitude)
                .orElse(null);

        if (existingLocation != null) {
            log.info(SERVICE + "기존 위치 발견: {}", existingLocation.getId());
            return buildWeatherAPILocation(existingLocation);
        }

        // 2. 새 위치 생성 및 저장 (위도/경도)
        Location newLocation = Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .x(0)
                .y(0)
                .locationNames(List.of("UNKNOWN"))
                .build();

        Location savedLocation = locationRepository.save(newLocation);
        log.info(SERVICE + "새 위치 저장 완료: {}", savedLocation.getId());

        // 3. 카카오 API 호출로 해당 좌표 행정구역명 받아온 후 Location 엔티티로 저장
        try {
            log.info(SERVICE + "카카오 API 호출 시작: longitude={}, latitude={}", longitude, latitude);
            
            KakaoAddressResponse response = kakaoApiClient
                    .getRegionByCoordinates(longitude, latitude)
                    .block();

            log.info(SERVICE + "카카오 API 응답: {}", response);

            if (response != null && response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                log.info(SERVICE + "카카오 API 문서 개수: {}", response.getDocuments().size());

                // 행정동 우선, 없으면 법정동 사용
                var document = response.getDocuments().stream()
                        .filter(doc -> "H".equals(doc.getRegionType())) // 행정동 우선
                        .findFirst()
                        .orElse(response.getDocuments().get(0)); // 없으면 첫 번째

                log.info(SERVICE + "선택된 문서: {}", document);

                // 카카오 API에서 받아온 x, y 좌표 추출
                double x = document.getX();
                double y = document.getY();
                log.info(SERVICE + "카카오 API x, y 좌표: x={}, y={}", x, y);

                // 카카오 API 응답에서 주소 정보 추출
                // Region1DepthName: 시/도, Region2DepthName: 시/군/구, Region3DepthName: 읍/면/동, Region4DepthName: 리/동
                List<String> locationNames = List.of(
                        document.getRegion1DepthName(), // 시/도
                        document.getRegion2DepthName(), // 시/군/구
                        document.getRegion3DepthName(), // 읍/면/동
                        document.getRegion4DepthName()  // 리/동
                );

                log.info(SERVICE + "파싱된 주소: {}", locationNames);

                // 주소 정보 및 좌표 업데이트
                savedLocation.setX((int) x);
                savedLocation.setY((int) y);
                savedLocation.setLocationNames(locationNames);
                savedLocation = locationRepository.save(savedLocation);

                log.info(SERVICE + "카카오 API 처리 완료: {}", savedLocation.getId());
            } else {
                log.warn(SERVICE + "카카오 API 응답이 비어있음: {}", response);
            }
        } catch (Exception e) {
            log.error("카카오 API 호출 실패", e);
        }

        // 5. 응답으로 locationNames에 행정구역명이 담긴 Location 엔티티 반환
        return buildWeatherAPILocation(savedLocation);
    }

    private WeatherAPILocation buildWeatherAPILocation(Location location) {
        return WeatherAPILocation.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .x(location.getX())
                .y(location.getY())
                .locationNames(location.getLocationNames())
                .build();
    }
}
