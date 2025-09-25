package com.samsamotot.otboo.location.integration;

import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.location.entity.WeatherAPILocation;
import com.samsamotot.otboo.location.repository.LocationRepository;
import com.samsamotot.otboo.location.service.LocationService;
import com.samsamotot.otboo.weather.controller.WeatherController;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.repository.GridRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "spring.datasource.hikari.max-lifetime=30000",
    "spring.datasource.hikari.connection-timeout=5000",
    "spring.datasource.hikari.validation-timeout=3000"
})
@ActiveProfiles("test")
@DisplayName("위치 서비스 핵심 로직 통합 테스트")
class LocationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private GridRepository gridRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private WeatherController weatherController;

    private final double TEST_LONGITUDE = 126.9780;
    private final double TEST_LATITUDE = 37.5665;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        gridRepository.deleteAll();
    }

    @Test
    @DisplayName("기존 유효한 위치가 있으면 데이터베이스에서 조회하여 반환한다")
    void 기존_유효한_위치_있으면_데이터베이스에서_조회하여_반환() {
        // Given
        Location existingLocation = LocationFixture.createValidLocation();

        // Grid 먼저 저장
        Grid savedGrid = gridRepository.save(existingLocation.getGrid());
        existingLocation.setGrid(savedGrid);

        existingLocation.setLongitude(TEST_LONGITUDE);
        existingLocation.setLatitude(TEST_LATITUDE);
        locationRepository.save(existingLocation);

        // When
        WeatherAPILocation result = locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.latitude()).isEqualTo(TEST_LATITUDE);
        assertThat(result.longitude()).isEqualTo(TEST_LONGITUDE);
        assertThat(result.x()).isEqualTo(savedGrid.getX());
        assertThat(result.y()).isEqualTo(savedGrid.getY());
        assertThat(result.locationNames()).isEqualTo(existingLocation.getLocationNames());
    }

    @Test
    @DisplayName("기존 위치가 없으면 예외가 발생한다")
    void 기존_위치_없으면_예외_발생() {
        // Given - 데이터베이스에 위치가 없는 상태

        // When & Then
        assertThatThrownBy(() -> 
            locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE)
        ).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("기존 위치가 오래된 경우 예외가 발생한다")
    void 기존_위치_오래된_경우_예외_발생() {
        // Given
        Location staleLocation = LocationFixture.createStaleLocation();
        Grid savedGrid = gridRepository.save(staleLocation.getGrid());
        staleLocation.setGrid(savedGrid);

        staleLocation.setLongitude(TEST_LONGITUDE);
        staleLocation.setLatitude(TEST_LATITUDE);
        locationRepository.save(staleLocation);

        // When & Then
        assertThatThrownBy(() -> 
            locationService.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE)
        ).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("컨트롤러를 통해 위치 정보 조회가 정상 동작한다")
    void 컨트롤러_위치_정보_조회_정상_동작() {
        // Given
        Location existingLocation = LocationFixture.createValidLocation();
        Grid grid = existingLocation.getGrid();
        existingLocation.setLongitude(TEST_LONGITUDE);
        existingLocation.setLatitude(TEST_LATITUDE);
        gridRepository.save(grid);
        locationRepository.save(existingLocation);

        // When
        ResponseEntity<WeatherAPILocation> response = weatherController.getCurrentLocation(TEST_LONGITUDE, TEST_LATITUDE);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        WeatherAPILocation result = response.getBody();
        assertThat(result.latitude()).isEqualTo(TEST_LATITUDE);
        assertThat(result.longitude()).isEqualTo(TEST_LONGITUDE);
        assertThat(result.x()).isEqualTo(grid.getX());
        assertThat(result.y()).isEqualTo(grid.getY());
        assertThat(result.locationNames()).isEqualTo(existingLocation.getLocationNames());
    }
}