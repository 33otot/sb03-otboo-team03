package com.samsamotot.otboo.weather.repository;

import com.samsamotot.otboo.common.config.QueryDslConfig;
import com.samsamotot.otboo.common.config.TestJpaAuditingConfig;
import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.fixture.GridFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
@DisplayName("Weather 레포지토리 슬라이스 테스트")
class WeatherRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private TestEntityManager em;

    private Grid grid;
    private User user;

    @BeforeEach
    void setUp() {
        grid = GridFixture.createGrid();
        em.persist(grid);

        user = UserFixture.createUser();
        em.persist(user);

        em.flush();
    }

    @Test
    @DisplayName("findAllByGrid - 특정 격자의 날씨 정보를 모두 조회한다")
    void findAllByGrid_Success() {
        // given
        Weather weather1 = WeatherFixture.createWeather(grid);
        Weather weather2 = WeatherFixture.createWeather(grid);
        weather2.setForecastAt(Instant.now().plus(1, ChronoUnit.HOURS));
        em.persist(weather1);
        em.persist(weather2);
        em.flush();
        em.clear();

        // when
        List<Weather> result = weatherRepository.findAllByGrid(grid);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findTopByGridAndForecastAtOrderByForecastedAtDesc - 특정 격자/예보 시각의 최신 날씨를 조회한다")
    void findTopByGridAndForecastAtOrderByForecastedAtDesc_Success() {
        // given
        Instant forecastAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant earlyForecasted = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant lateForecasted = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        Weather oldWeather = WeatherFixture.createWeather(grid);
        oldWeather.setForecastAt(forecastAt);
        oldWeather.setForecastedAt(earlyForecasted);

        Weather newWeather = WeatherFixture.createWeather(grid);
        newWeather.setForecastAt(forecastAt);
        newWeather.setForecastedAt(lateForecasted);

        em.persist(oldWeather);
        em.persist(newWeather);
        em.flush();

        // when
        Optional<Weather> result = weatherRepository.findTopByGridAndForecastAtOrderByForecastedAtDesc(grid, forecastAt);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getForecastedAt()).isEqualTo(lateForecasted);
    }

    @Test
    @DisplayName("deleteOldAndUnreferencedWeather - 참조 없는 오래된 날씨 데이터를 삭제한다")
    void deleteOldAndUnreferencedWeather_Success() {
        // given
        Instant threshold = Instant.now().truncatedTo(ChronoUnit.HOURS);
        
        // 기준 시각 이전이면서 피드 참조가 없는 날씨 (삭제 대상)
        Weather targetWeather = WeatherFixture.createWeather(grid);
        targetWeather.setForecastAt(threshold.minus(1, ChronoUnit.HOURS));
        em.persist(targetWeather);

        // 기준 시각 이전이지만 피드가 참조하는 날씨 (삭제 안 됨)
        Weather referencedWeather = WeatherFixture.createWeather(grid);
        referencedWeather.setForecastAt(threshold.minus(2, ChronoUnit.HOURS));
        em.persist(referencedWeather);
        Feed feed = FeedFixture.createFeed(user, referencedWeather);
        em.persist(feed);

        // 기준 시각 이후인 날씨 (삭제 안 됨)
        Weather futureWeather = WeatherFixture.createWeather(grid);
        futureWeather.setForecastAt(threshold.plus(1, ChronoUnit.HOURS));
        em.persist(futureWeather);

        em.flush();
        em.clear();

        // when
        int deletedCount = weatherRepository.deleteOldAndUnreferencedWeather(grid, threshold);

        // then
        assertThat(deletedCount).isEqualTo(1);
        List<Weather> remaining = weatherRepository.findAll();
        assertThat(remaining).hasSize(2);
    }

    @Test
    @DisplayName("deleteOutdatedAndUnreferencedWeather - 대체된 오래된 발표의 날씨 데이터를 삭제한다")
    void deleteOutdatedAndUnreferencedWeather_Success() {
        // given
        Instant forecastAt = Instant.now().truncatedTo(ChronoUnit.HOURS);
        
        // 동일 예보 시각에 대해 오래된 발표 날씨 (삭제 대상)
        Weather outdatedWeather = WeatherFixture.createWeather(grid);
        outdatedWeather.setForecastAt(forecastAt);
        outdatedWeather.setForecastedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        em.persist(outdatedWeather);

        // 동일 예보 시각에 대해 더 최신 발표 날씨 (삭제 안 됨)
        Weather latestWeather = WeatherFixture.createWeather(grid);
        latestWeather.setForecastAt(forecastAt);
        latestWeather.setForecastedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        em.persist(latestWeather);

        // 동일 예보 시각에 대해 오래된 발표 날씨이지만 피드가 참조하는 날씨 (삭제 안 됨)
        Weather outdatedReferencedWeather = WeatherFixture.createWeather(grid);
        outdatedReferencedWeather.setForecastAt(forecastAt.plus(1, ChronoUnit.HOURS));
        outdatedReferencedWeather.setForecastedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        em.persist(outdatedReferencedWeather);
        
        // 위 객체와 동일 예보 시각을 갖는 더 최신 발표 날씨
        Weather newerForReferenced = WeatherFixture.createWeather(grid);
        newerForReferenced.setForecastAt(forecastAt.plus(1, ChronoUnit.HOURS));
        newerForReferenced.setForecastedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        em.persist(newerForReferenced);

        Feed feed = FeedFixture.createFeed(user, outdatedReferencedWeather);
        em.persist(feed);

        em.flush();
        em.clear();

        // when
        int deletedCount = weatherRepository.deleteOutdatedAndUnreferencedWeather(grid);

        // then
        assertThat(deletedCount).isEqualTo(1); // outdatedWeather만 삭제됨
        List<Weather> remaining = weatherRepository.findAll();
        assertThat(remaining).hasSize(3);
    }
}
