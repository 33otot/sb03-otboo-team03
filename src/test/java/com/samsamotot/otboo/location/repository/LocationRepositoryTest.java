package com.samsamotot.otboo.location.repository;

import com.samsamotot.otboo.common.config.QueryDslConfig;
import com.samsamotot.otboo.common.config.TestJpaAuditingConfig;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.weather.entity.Grid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.samsamotot.otboo")
@Import({TestJpaAuditingConfig.class, QueryDslConfig.class})
@Testcontainers
@TestPropertySource(properties = {
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@DisplayName("Location 레포지토리 슬라이스 테스트")
class LocationRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private TestEntityManager em;

    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = GridFixture.createGrid();
        em.persist(grid);
        em.flush();
    }

    @Test
    @DisplayName("findByLongitudeAndLatitude - 위도와 경도로 위치 정보를 조회한다")
    void findByLongitudeAndLatitude_Success() {
        // given
        Location location = Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .grid(grid)
                .locationNames(List.of("서울특별시", "중구"))
                .build();
        em.persist(location);
        em.flush();
        em.clear();

        // when
        Optional<Location> result = locationRepository.findByLongitudeAndLatitude(126.9780, 37.5665);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getLatitude()).isEqualTo(37.5665);
        assertThat(result.get().getLongitude()).isEqualTo(126.9780);
    }

    @Test
    @DisplayName("findAllWithGrid - 격자(Grid) 정보를 페치 조인하여 모든 위치 정보를 조회한다")
    void findAllWithGrid_Success() {
        // given
        Location location1 = Location.builder()
                .latitude(37.5665)
                .longitude(126.9780)
                .grid(grid)
                .locationNames(List.of("서울특별시", "중구"))
                .build();
        Location location2 = Location.builder()
                .latitude(35.1796)
                .longitude(129.0756)
                .grid(grid)
                .locationNames(List.of("부산광역시", "연제구"))
                .build();
        em.persist(location1);
        em.persist(location2);
        em.flush();
        em.clear();

        // when
        List<Location> result = locationRepository.findAllWithGrid();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getGrid()).isNotNull();
    }
}
