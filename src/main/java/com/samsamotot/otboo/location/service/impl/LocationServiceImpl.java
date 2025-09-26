package com.samsamotot.otboo.location.service.impl;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.location.client.KakaoApiClient;
import com.samsamotot.otboo.location.dto.KakaoAddressResponse;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.repository.GridRepository;
import com.samsamotot.otboo.weather.util.KmaGridConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private static final String SERVICE_NAME = "[LocationServiceImpl] ";

    private final LocationRepository locationRepository;
    private final KakaoApiClient kakaoApiClient;
    private final GridRepository gridRepository;

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
        log.info(SERVICE_NAME + "위치 조회 시작: longitude={}, latitude={}", longitude, latitude);

        // 기존 위치 찾기
        Location location = locationRepository.findByLongitudeAndLatitude(longitude, latitude)
                .orElse(null);

        if (location != null) {
            // 기존 위치가 있는 경우
            if (isLocationStale(location)) {
                log.info(SERVICE_NAME + "기존 위치 데이터가 오래됨, 카카오 API로 갱신: {}", location.getId());
                location = updateLocation(location, longitude, latitude);
            } else {
                log.info(SERVICE_NAME + "기존 위치 데이터 유효, 재사용: {}", location.getId());
            }
        } else {
            // 기존 위치가 없는 경우 - 새로 생성
            log.info(SERVICE_NAME + "기존 위치 없음, 카카오 API로 새 위치 생성");
            location = createLocation(longitude, latitude);
        }

        Grid grid = location.getGrid();

        WeatherAPILocation locationDto = WeatherAPILocation.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .x(grid.getX())
                .y(grid.getY())
                .locationNames(location.getLocationNames())
                .build();

        return locationDto;
    }

    private boolean isLocationStale(Location location) {
        if (location == null) {
            return true;
        }

        if (location.getLocationNames() == null || location.getGrid() == null) {
            return true;
        }

        // x, y가 0인 경우 갱신 필요
        if (location.getGrid().getX() == 0 || location.getGrid().getY() == 0) {
            return true;
        }

        // 생성된지 한달 넘었는지 검증
        if (location.getCreatedAt() != null) {
            Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
            if (location.getCreatedAt().isBefore(oneMonthAgo)) {
                return true;
            }
        }

        // locationNames에 "UNKNOWN"이 포함되거나, null이 포함된 경우
        return location.getLocationNames().contains("UNKNOWN") ||
                location.getLocationNames().stream().anyMatch(Objects::isNull);
    }

    private Location updateLocation(Location location, double longitude, double latitude) {
        KakaoAddressResponse response = callKakaoApi(longitude, latitude);


        // WGS84 좌표를 기상청 격자 좌표로 변환
        KmaGridConverter.GridPoint gridPoint = KmaGridConverter.toGrid(latitude, longitude);
        Grid grid = findOrCreateGrid(gridPoint);

        // 기존 위치 업데이트
        location.setGrid(grid);
        location.setLocationNames(parseLocationNames(response));



        Location savedLocation = locationRepository.save(location);
        log.info(SERVICE_NAME + "기존 위치 업데이트 완료: {}", savedLocation.getId());
        return savedLocation;
    }

    private Location createLocation(double longitude, double latitude) {
        KakaoAddressResponse response = callKakaoApi(longitude, latitude);

        // WGS84 좌표를 기상청 격자 좌표로 변환
        KmaGridConverter.GridPoint gridPoint = KmaGridConverter.toGrid(latitude, longitude);

        Grid grid = findOrCreateGrid(gridPoint);

        // 새 위치 생성
        Location newLocation = Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .grid(grid)
                .locationNames(parseLocationNames(response))
                .build();

        Location savedLocation = locationRepository.save(newLocation);
        log.info(SERVICE_NAME + "새 위치 생성 완료: {}", savedLocation.getId());
        return savedLocation;
    }

    private KakaoAddressResponse callKakaoApi(double longitude, double latitude) {
        try {
            log.info(SERVICE_NAME + "카카오 API 호출 시작: longitude={}, latitude={}", longitude, latitude);

            KakaoAddressResponse response = kakaoApiClient
                    .getRegionByCoordinates(longitude, latitude)
                    .block(Duration.ofSeconds(5));

            if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
                log.warn(SERVICE_NAME + "카카오 API 응답이 비어있음");
                throw new OtbooException(ErrorCode.API_NO_RESPONSE);
            }

            log.info(SERVICE_NAME + "카카오 API 호출 성공");
            return response;

        } catch (OtbooException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 API 호출 실패", e);
            throw new OtbooException(ErrorCode.API_CALL_ERROR);
        }
    }

    private Grid findOrCreateGrid(KmaGridConverter.GridPoint gridPoint) {
        // 1. 먼저 조회를 시도합니다.
        Optional<Grid> gridOpt = gridRepository.findByXAndY(gridPoint.nx(), gridPoint.ny());
        if (gridOpt.isPresent()) {
            return gridOpt.get();
        }

        // 2. 조회가 안되면 생성을 시도합니다. 동시성 문제를 고려하여 try-catch로 감쌉니다.
        try {
            log.info(SERVICE_NAME + "새로운 Grid 생성 시도: x={}, y={}", gridPoint.nx(), gridPoint.ny());
            Grid newGrid = Grid.builder().x(gridPoint.nx()).y(gridPoint.ny()).build();
            return gridRepository.save(newGrid);
        } catch (DataIntegrityViolationException e) {
            // 다른 트랜잭션에서 거의 동시에 생성하여 UNIQUE 제약조건 위반이 발생한 경우, 다시 조회하여 반환합니다.
            log.warn(SERVICE_NAME + "Grid 동시 생성 충돌 발생, 기존 Grid를 다시 조회합니다: x={}, y={}", gridPoint.nx(), gridPoint.ny());
            return gridRepository.findByXAndY(gridPoint.nx(), gridPoint.ny())
                    .orElseThrow(() -> new IllegalStateException("Failed to find grid after integrity violation. This should not happen."));
        }
    }

    private List<String> parseLocationNames(KakaoAddressResponse response) {
        // 카카오 API 응답 파싱 (행정동 우선)
        var document = response.getDocuments().stream()
                .filter(doc -> "H".equals(doc.getRegionType()))
                .findFirst()
                .orElse(response.getDocuments().get(0));

        return List.of(
                document.getRegion1DepthName(), document.getRegion2DepthName(),
                document.getRegion3DepthName(), document.getRegion4DepthName()
        ).stream().filter(Objects::nonNull).toList();
    }
}
