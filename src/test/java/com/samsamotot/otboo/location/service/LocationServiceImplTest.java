package com.samsamotot.otboo.location.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.dto.KakaoAddressResponseFixture;
import com.samsamotot.otboo.location.client.KakaoApiClient;
import com.samsamotot.otboo.location.dto.KakaoAddressResponse;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.location.service.impl.LocationServiceImpl;
import com.samsamotot.otboo.weather.dto.WeatherAPILocation;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.repository.GridRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationServiceImpl 단위 테스트")
class LocationServiceImplTest {

    @Mock
    private GridRepository gridRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private KakaoApiClient kakaoApiClient;

    @InjectMocks
    private LocationServiceImpl locationService;

    private final double TEST_LONGITUDE = 126.9780;
    private final double TEST_LATITUDE = 37.5665;

    @Nested
    @DisplayName("getCurrentLocation 메소드 테스트")
    class GerCurrentLocationTest {
        @Test
        @DisplayName("기존 위치가 있고 유효한 경우 해당 위치를 반환한다")
        void 기존_유효한_위치_있으면_해당_위치를_반환() {
            // Given
            Location existingLocation = LocationFixture.createValidLocation();
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.of(existingLocation));
            Grid existingLocationGrid = existingLocation.getGrid();

            // When
            WeatherAPILocation result = locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.latitude()).isEqualTo(existingLocation.getLatitude());
            assertThat(result.longitude()).isEqualTo(existingLocation.getLongitude());
            assertThat(result.x()).isEqualTo(existingLocationGrid.getX());
            assertThat(result.y()).isEqualTo(existingLocationGrid.getY());
            assertThat(result.locationNames()).isEqualTo(existingLocation.getLocationNames());

            // 카카오 API 호출하지 않음
            verify(kakaoApiClient, never()).getRegionByCoordinates(anyDouble(), anyDouble());
            verify(locationRepository, never()).save(any(Location.class));
        }

        @Test
        @DisplayName("기존 위치가 있지만 오래된 경우 카카오 API로 갱신한다")
        void 기존_오래된_위치가_있으면_카카오_API로_갱신() {
            // Given
            Location staleLocation = LocationFixture.createStaleLocation();
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.of(staleLocation));

            KakaoAddressResponse kakaoResponse = KakaoAddressResponseFixture.createKakaoAddressResponse();
            when(kakaoApiClient.getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Mono.just(kakaoResponse));

            when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(staleLocation);

            // Grid Mock 설정 추가
            Grid mockGrid = GridFixture.createGrid(); // 60, 127
            when(gridRepository.findByXAndY(60, 127))
                    .thenReturn(Optional.of(mockGrid)); // findByXAndY 먼저 설정

            // When
            WeatherAPILocation result = locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.longitude()).isEqualTo(TEST_LONGITUDE);
            assertThat(result.latitude()).isEqualTo(TEST_LATITUDE);
            assertThat(result.x()).isEqualTo(60);
            assertThat(result.y()).isEqualTo(127);

            // 검증
            verify(locationRepository).findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE);
            verify(kakaoApiClient).getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE);
            verify(gridRepository).findByXAndY(60, 127); // Grid 조회 검증 추가
            verify(locationRepository).saveAndFlush(staleLocation);
        }

        @Test
        @DisplayName("기존 위치가 없는 경우 카카오 API로 새 위치를 생성한다")
        void 기존_위치_없으면_카카오_API로_새_위치_생성() {
            // Given
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.empty());

            KakaoAddressResponse kakaoResponse = KakaoAddressResponseFixture.createKakaoAddressResponse();
            when(kakaoApiClient.getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Mono.just(kakaoResponse));

            Grid mockGrid = GridFixture.createGrid();
            when(gridRepository.findByXAndY(60, 127))
                    .thenReturn(Optional.of(mockGrid)); // findByXAndY 먼저 설정

            Location savedLocation = LocationFixture.createValidLocation();
            when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(savedLocation);

            // When
            WeatherAPILocation result = locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.latitude()).isEqualTo(TEST_LATITUDE);
            assertThat(result.longitude()).isEqualTo(TEST_LONGITUDE);
            assertThat(result.x()).isEqualTo(60);
            assertThat(result.y()).isEqualTo(127);
            assertThat(result.locationNames()).containsExactly("서울특별시", "중구", "명동", "");

            // 검증
            verify(locationRepository).findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE);
            verify(kakaoApiClient).getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE);
            verify(gridRepository).findByXAndY(60, 127);
            verify(locationRepository).saveAndFlush(any(Location.class));
        }

        @Test
        @DisplayName("카카오 API 응답이 비어있는 경우 예외가 발생한다")
        void 카카오_API_응답이_비어있으면_예외_발생() {
            // Given
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.empty());

            KakaoAddressResponse emptyResponse = KakaoAddressResponseFixture.createEmptyResponse();
            when(kakaoApiClient.getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Mono.just(emptyResponse));

            // When & Then
            assertThatThrownBy(() ->
                    locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE)
            )
                    .isInstanceOf(OtbooException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.API_NO_RESPONSE);

            // 검증
            verify(kakaoApiClient).getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE);
        }

        @Test
        @DisplayName("카카오 API 호출이 실패하는 경우 예외가 발생한다")
        void 카카오_API_호출이_실패하면_예외_발생() {
            // Given
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.empty());

            when(kakaoApiClient.getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Mono.error(new RuntimeException("API Error")));

            // When & Then
            assertThatThrownBy(() ->
                    locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE)
            )
                    .isInstanceOf(OtbooException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.API_CALL_ERROR);

            // 검증
            verify(kakaoApiClient).getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE);
        }

        @Test
        @DisplayName("기존 위치가 UNKNOWN을 포함하는 경우 갱신한다")
        void 기존_위치가_UNKNOWN을_포함하면_갱신() {
            // Given
            Location locationWithUnknown = LocationFixture.createLocationWithUnknown();
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.of(locationWithUnknown));

            KakaoAddressResponse kakaoResponse = KakaoAddressResponseFixture.createKakaoAddressResponse();
            when(kakaoApiClient.getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Mono.just(kakaoResponse));

            Grid mockGrid = GridFixture.createGrid();
            when(gridRepository.findByXAndY(60, 127))
                    .thenReturn(Optional.of(mockGrid));

            when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(locationWithUnknown);

            // When
            WeatherAPILocation result = locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

            // Then
            assertThat(result).isNotNull();

            // 검증
            verify(kakaoApiClient).getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE);
            verify(gridRepository).findByXAndY(60, 127);
            verify(locationRepository).saveAndFlush(locationWithUnknown);
        }

        @Test
        @DisplayName("기존 위치가 x, y가 0인 경우 갱신한다")
        void 기존_위치가_x_y가_0이면_갱신() {
            // Given
            Location locationWithZeroCoords = LocationFixture.createLocationWithZeroCoordinates();
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.of(locationWithZeroCoords));

            KakaoAddressResponse kakaoResponse = KakaoAddressResponseFixture.createKakaoAddressResponse();
            when(kakaoApiClient.getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Mono.just(kakaoResponse));

            Grid mockGrid = GridFixture.createGrid();
            when(gridRepository.findByXAndY(60, 127))
                    .thenReturn(Optional.of(mockGrid));

            when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(locationWithZeroCoords);

            // When
            WeatherAPILocation result = locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

            // Then
            assertThat(result).isNotNull();

            // 검증
            verify(kakaoApiClient).getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE);
            verify(gridRepository).findByXAndY(60, 127);
            verify(locationRepository).saveAndFlush(locationWithZeroCoords);
        }
    }

    @Nested
    @DisplayName("주소 파싱 로직 테스트")
    class parseAddressTests {
        @Test
        @DisplayName("행정동이 있는 경우 행정동을 우선 선택한다")
        void 행정동이_있으면_행정동을_우선_선택() {
            // Given
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.empty());

            KakaoAddressResponse response = KakaoAddressResponseFixture.createResponseWithAdministrativeDistrict();
            when(kakaoApiClient.getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Mono.just(response));

            Location savedLocation = LocationFixture.createValidLocation();
            when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(savedLocation);

            // When
            WeatherAPILocation result = locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

            // Then
            assertThat(result).isNotNull();
            // 행정동이 선택되어야 함 (명동, not 을지로)
            assertThat(result.locationNames()).containsExactly("서울특별시", "중구", "명동", "");

            // 검증
            verify(kakaoApiClient).getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE);
        }

        @Test
        @DisplayName("행정동이 없는 경우 첫 번째 문서를 선택한다")
        void 행정동_없으면_첫_번째_문서_선택() {
            // Given
            when(locationRepository.findByLongitudeAndLatitude(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Optional.empty());

            KakaoAddressResponse response = KakaoAddressResponseFixture.createResponseWithoutAdministrativeDistrict();
            when(kakaoApiClient.getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE))
                    .thenReturn(Mono.just(response));

            Location savedLocation = LocationFixture.createValidLocation();
            when(locationRepository.saveAndFlush(any(Location.class))).thenReturn(savedLocation);

            // When
            WeatherAPILocation result = locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

            // Then
            assertThat(result).isNotNull();
            // 첫 번째 문서가 선택되어야 함 (명동)
            assertThat(result.locationNames()).containsExactly("서울특별시", "중구", "명동", "");

            // 검증
            verify(kakaoApiClient).getRegionByCoordinates(TEST_LONGITUDE, TEST_LATITUDE);
        }
    }

    @Nested
    @DisplayName("isLocationStale 메소드 테스트")
    class IsLocationStaleTest {

        private Method isLocationStaleMethod;

        // Reflection을 사용하여 private 메소드에 접근 가능하도록 설정
        IsLocationStaleTest() throws NoSuchMethodException {
            isLocationStaleMethod = LocationServiceImpl.class.getDeclaredMethod("isLocationStale", Location.class);
            isLocationStaleMethod.setAccessible(true);
        }

        @Test
        @DisplayName("유효한 Location 객체는 stale 하지 않아야 한다")
        void 유효한_Location은_stale하지_않음() throws Exception {
            // Given
            Location validLocation = LocationFixture.createValidLocation();

            // When
            boolean result = (boolean) isLocationStaleMethod.invoke(locationService, validLocation);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("생성된지 31일 지난 Location은 stale 해야 한다")
        void 오래된_Location은_stale함() throws Exception {
            // Given
            Location location = Location.builder()
                    .grid(GridFixture.createGrid())
                    .locationNames(List.of("서울", "강남"))
                    .build();

            Field createdAtField = location.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(location, Instant.now().minus(31, ChronoUnit.DAYS));

            // When
            boolean result = (boolean) isLocationStaleMethod.invoke(locationService, location);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Grid가 null인 Location은 stale 해야 한다")
        void Grid가_null이면_stale함() throws Exception {
            // Given
            Location location = Location.builder()
                    .grid(null)
                    .locationNames(List.of("서울", "강남"))
                    .build();

            // When
            boolean result = (boolean) isLocationStaleMethod.invoke(locationService, location);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("LocationNames가 null인 Location은 stale 해야 한다")
        void LocationNames가_null이면_stale함() throws Exception {
            // Given
            Location location = Location.builder()
                    .grid(GridFixture.createGrid())
                    .locationNames(null)
                    .build();

            // When
            boolean result = (boolean) isLocationStaleMethod.invoke(locationService, location);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("LocationNames에 UNKNOWN이 포함된 Location은 stale 해야 한다")
        void LocationNames에_UNKNOWN이_포함되면_stale함() throws Exception {
            // Given
            Location location = LocationFixture.createLocationWithUnknown();

            // When
            boolean result = (boolean) isLocationStaleMethod.invoke(locationService, location);

            // Then
            assertThat(result).isTrue();
        }
    }

}