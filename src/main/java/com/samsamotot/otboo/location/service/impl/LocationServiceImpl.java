package com.samsamotot.otboo.location.service.impl;

import com.samsamotot.otboo.common.exception.ErrorCode;
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
import java.util.Objects;

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
     *   <li>카카오 API를 호출하여 최신 위치 정보로 업데이트/생성</li>
     *   <li>업데이트된 위치 정보를 반환</li>
     * </ol>
     * 
     * <p>카카오 API 호출 시 주소 우선순위:</p>
     * <ul>
     *   <li>1순위: 행정동 (RegionType = "H")</li>
     *   <li>2순위: 법정동 (RegionType = "B")</li>
     * </ul>
     * 
     * <p>카카오 API 호출 실패 시 예외가 발생합니다.</p>
     * 
     * @param longitude 경도 (WGS84 좌표계)
     * @param latitude  위도 (WGS84 좌표계)
     * @return WeatherAPILocation 위치 정보 객체
     * @throws OtbooException 카카오 API 호출 실패 시
     * 
     * @since 1.0
     */
    @Override
    @Transactional
    public WeatherAPILocation getCurrentLocation(double longitude, double latitude) {
        log.info(SERVICE + "위치 조회 시작: longitude={}, latitude={}", longitude, latitude);

        // 기존 위치 찾기
        Location location = locationRepository.findByLongitudeAndLatitude(longitude, latitude)
                .orElse(null);

        if (location != null) {
            // 기존 위치가 있는 경우
            if (isLocationStale(location)) {
                log.info(SERVICE + "기존 위치 데이터가 오래됨, 카카오 API로 갱신: {}", location.getId());
                location = updateLocation(location, longitude, latitude);
            } else {
                log.info(SERVICE + "기존 위치 데이터 유효, 재사용: {}", location.getId());
            }
        } else {
            // 기존 위치가 없는 경우 - 새로 생성
            log.info(SERVICE + "기존 위치 없음, 카카오 API로 새 위치 생성");
            location = createLocation(longitude, latitude);
        }
        
        return buildWeatherAPILocation(location);
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

    private boolean isLocationStale(Location location) {
        // locationNames가 null이거나, UNKNOWN이 포함되어 있거나, null이 포함된 경우, x 혹은 y가 0인 경우
        if (location.getLocationNames() == null) {
            return true;
        }
        
        return location.getLocationNames().contains("UNKNOWN") ||
                location.getLocationNames().stream().anyMatch(Objects::isNull) ||
                location.getX() == 0 || location.getY() == 0;
    }

    private Location updateLocation(Location location, double longitude, double latitude) {
        KakaoAddressResponse response = callKakaoApi(longitude, latitude);
        
        // 카카오 API 응답 파싱
        var document = response.getDocuments().stream()
                .filter(doc -> "H".equals(doc.getRegionType()))
                .findFirst()
                .orElse(response.getDocuments().get(0));

        // 기존 위치 업데이트
        location.setX((int) document.getX());
        location.setY((int) document.getY());
        location.setLocationNames(List.of(
                document.getRegion1DepthName(),
                document.getRegion2DepthName(),
                document.getRegion3DepthName(),
                document.getRegion4DepthName()
        ));
        
        Location savedLocation = locationRepository.save(location);
        log.info(SERVICE + "기존 위치 업데이트 완료: {}", savedLocation.getId());
        return savedLocation;
    }

    private Location createLocation(double longitude, double latitude) {
        KakaoAddressResponse response = callKakaoApi(longitude, latitude);
        
        // 카카오 API 응답 파싱
        var document = response.getDocuments().stream()
                .filter(doc -> "H".equals(doc.getRegionType()))
                .findFirst()
                .orElse(response.getDocuments().get(0));

        // 새 위치 생성
        Location newLocation = Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .x((int) document.getX())
                .y((int) document.getY())
                .locationNames(List.of(
                        document.getRegion1DepthName(),
                        document.getRegion2DepthName(),
                        document.getRegion3DepthName(),
                        document.getRegion4DepthName()
                ))
                .build();
        
        Location savedLocation = locationRepository.save(newLocation);
        log.info(SERVICE + "새 위치 생성 완료: {}", savedLocation.getId());
        return savedLocation;
    }

    private KakaoAddressResponse callKakaoApi(double longitude, double latitude) {
        try {
            log.info(SERVICE + "카카오 API 호출 시작: longitude={}, latitude={}", longitude, latitude);
            
            KakaoAddressResponse response = kakaoApiClient
                    .getRegionByCoordinates(longitude, latitude)
                    .block();

            if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
                log.warn(SERVICE + "카카오 API 응답이 비어있음");
                throw new OtbooException(ErrorCode.API_NO_RESPONSE);
            }

            log.info(SERVICE + "카카오 API 호출 성공");
            return response;
            
        } catch (OtbooException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 API 호출 실패", e);
            throw new OtbooException(ErrorCode.API_CALL_ERROR);
        }
    }
}
