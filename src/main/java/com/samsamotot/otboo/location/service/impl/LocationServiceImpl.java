package com.samsamotot.otboo.location.service.impl;

import com.samsamotot.otboo.location.client.KakaoApiClient;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.entity.WeatherAPILocation;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.weather.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final String SERVICE = "LocationServiceImpl";

    private final LocationRepository locationRepository;
    private final KakaoApiClient kakaoApiClient;

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

                List<String> locationNames = List.of(
                        document.getRegion1DepthName(), // 시/도
                        document.getRegion2DepthName(), // 시/군/구
                        document.getRegion3DepthName(), // 읍/면/동
                        document.getRegion4DepthName()  // 리/동
                );

                log.info(SERVICE + "파싱된 주소: {}", locationNames);

                // 주소 정보 업데이트
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
