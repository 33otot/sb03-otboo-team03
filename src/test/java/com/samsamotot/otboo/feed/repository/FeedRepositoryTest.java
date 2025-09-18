package com.samsamotot.otboo.feed.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samsamotot.otboo.common.config.QuerydslConfig;
import com.samsamotot.otboo.common.config.TestJpaAuditingConfig;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.samsamotot.otboo")
@Import({TestJpaAuditingConfig.class, QuerydslConfig.class})
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@DisplayName("Feed 레포지토리 슬라이스 테스트")
public class FeedRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url",      postgres::getJdbcUrl);
        reg.add("spring.datasource.username", postgres::getUsername);
        reg.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private TestEntityManager em;

    Instant baseTime = Instant.now().minusSeconds(3000);
    User author;
    Location location;
    Weather weather;

    @BeforeEach
    void setUp() {
        author = UserFixture.createUser();
        em.persist(author);
        location = LocationFixture.createLocation();
        em.persist(location);
        weather = WeatherFixture.createWeather(location);
        em.persist(weather);

        em.flush();
        em.clear();
    }

    @Test
    void createdAt_내림차순으로_피드를_조회한다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;

        for (int i = 0; i < 5; i ++) {
            Feed feed = FeedFixture.createFeed(author, weather);
            em.persist(feed);
        }
        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null,
            null,
            limit,
            sortBy,
            sortDirection,
            null,
            null,
            null,
            null
        );

        // then
        assertThat(result).hasSize(limit);
        assertThat(result).isSortedAccordingTo(
            Comparator.comparing(Feed::getCreatedAt).reversed()
        );
    }

    @Test
    void createdAt_오름차순으로_피드를_조회한다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.ASCENDING;

        for (int i = 0; i < 5; i ++) {
            Feed feed = FeedFixture.createFeedWithCreatedAt(author, weather, baseTime.plusSeconds(i * 1000));
            em.persist(feed);
        }
        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null,
            null,
            limit,
            sortBy,
            sortDirection,
            null,
            null,
            null,
            null
        );

        // then
        assertThat(result).hasSize(limit);
        assertThat(result).isSortedAccordingTo(
            Comparator.comparing(Feed::getCreatedAt)
        );
    }

    @Test
    void likeCount_내림차순으로_피드를_조회한다() {

        // given
        int limit = 5;
        String sortBy = "likeCount";
        SortDirection sortDirection = SortDirection.DESCENDING;

        for (int i = 0; i < 5; i ++) {
            Feed feed = FeedFixture.createFeedWithLikeCount(author, weather, (long) i);
            em.persist(feed);
        }
        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null,
            null,
            limit,
            sortBy,
            sortDirection,
            null,
            null,
            null,
            null
        );

        // then
        assertThat(result).hasSize(limit);
        assertThat(result).isSortedAccordingTo(
            Comparator.comparing(Feed::getLikeCount).reversed()
        );
    }

    @Test
    void likeCount_오름차순으로_피드를_조회한다() {

        // given
        int limit = 5;
        String sortBy = "likeCount";
        SortDirection sortDirection = SortDirection.ASCENDING;

        for (int i = 0; i < 5; i ++) {
            Feed feed = FeedFixture.createFeedWithLikeCount(author, weather, (long) i);
            em.persist(feed);
        }
        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null,
            null,
            limit,
            sortBy,
            sortDirection,
            null,
            null,
            null,
            null
        );

        // then
        assertThat(result).hasSize(limit);
        assertThat(result).isSortedAccordingTo(
            Comparator.comparing(Feed::getLikeCount)
        );
    }

    @Test
    void 커서가_주어졌을_때_이후_데이터만_조회된다() {

        // given
        int limit = 5;
        String sortBy = "likeCount";
        SortDirection sortDirection = SortDirection.ASCENDING;

        List<Feed> allFeeds = new ArrayList<>();
        for (int i = 0; i < 5; i ++) {
            Feed feed = FeedFixture.createFeedWithLikeCount(author, weather, (long) i);
            em.persist(feed);
            allFeeds.add(feed);
        }
        em.flush();
        em.clear();

        Feed cursorFeed = allFeeds.get(2);
        String cursor = String.valueOf(cursorFeed.getLikeCount());
        UUID idAfter = cursorFeed.getId();

        // when
        List<Feed> result = feedRepository.findByCursor(
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            null,
            null,
            null,
            null
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(Feed::getLikeCount)
            .containsExactly(3L, 4L);
    }

    @Test
    void 잘못된_정렬기준_입력시_예외가_발생한다() {

        // given
        int limit = 5;
        String invalidSortBy = "invalidSort";
        SortDirection sortDirection = SortDirection.ASCENDING;

        // when & then
        assertThatThrownBy(() -> feedRepository.findByCursor(
            null,
            null,
            limit,
            invalidSortBy,
            sortDirection,
            null,
            null,
            null,
            null
        ))
            .isInstanceOf(OtbooException.class)
            .extracting(e -> ((OtbooException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_SORT_FIELD);
    }

    @Test
    void 키워드_조건으로_검색시_해당_키워드가_포함된_피드만_반환된다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;
        String keyword = "날씨";
        int count = 5;

        for (int i = 0 ; i < count; i ++) {
            Feed feedWithKeyword = FeedFixture.createFeedWithKeyword(author, weather, keyword + i);
            Feed feedWithOtherKeyword = FeedFixture.createFeedWithKeyword(author, weather, "계절" + i);
            em.persist(feedWithKeyword);
            em.persist(feedWithOtherKeyword);
        }

        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null, null, limit + 1, sortBy, sortDirection, keyword, null, null, null
        );

        // then
        assertThat(result).hasSize(count);
        assertThat(result).allSatisfy(feed -> {
            assertThat(feed.getContent()).contains(keyword);
        });
    }

    @Test
    void 하늘상태_필터_적용시_지정_상태의_피드만_반환된다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;
        SkyStatus skyStatus = SkyStatus.CLEAR;
        int count = 5;

        Weather targetWeather = Weather.builder()
            .forecastAt(Instant.now())
            .forecastedAt(Instant.now())
            .location(location)
            .skyStatus(skyStatus)
            .build();
        em.persist(targetWeather);

        for (int i = 0 ; i < count; i ++) {
            Feed feedWithSkyStatus = FeedFixture.createFeed(author, targetWeather);
            Feed feedWithOtherSkyStatus = FeedFixture.createFeed(author, weather);
            em.persist(feedWithSkyStatus);
            em.persist(feedWithOtherSkyStatus);
        }

        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null, null, limit + 1, sortBy, sortDirection, null, skyStatus, null, null
        );

        // then
        assertThat(result).hasSize(count);
        assertThat(result).allSatisfy(feed -> {
            assertThat(feed.getWeather().getSkyStatus()).isEqualTo(skyStatus);
        });
    }

    @Test
    void 강수유형_필터_적용시_지정_유형의_피드만_반환된다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;
        Precipitation precipitation = Precipitation.RAIN;
        int count = 5;

        Weather targetWeather = Weather.builder()
            .forecastAt(Instant.now())
            .forecastedAt(Instant.now())
            .location(location)
            .precipitationType(precipitation)
            .build();
        em.persist(targetWeather);

        for (int i = 0 ; i < count; i ++) {
            Feed feedWithPrecipitation = FeedFixture.createFeed(author, targetWeather);
            Feed feedWithOtherPrecipitation = FeedFixture.createFeed(author, weather);
            em.persist(feedWithPrecipitation);
            em.persist(feedWithOtherPrecipitation);
        }

        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null, null, limit + 1, sortBy, sortDirection, null, null, precipitation, null
        );

        // then
        assertThat(result).hasSize(count);
        assertThat(result).allSatisfy(feed -> {
            assertThat(feed.getWeather().getPrecipitationType()).isEqualTo(precipitation);
        });
    }

    @Test
    void 작성자ID_필터_적용시_해당_작성자의_피드만_반환된다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;
        int count = 5;

        User otherAuthor = User.builder()
            .username("test")
            .email("test@test.com")
            .password("test1234")
            .provider("local")
            .providerId(null)
            .role(Role.USER)
            .isLocked(false)
            .build();
        em.persist(otherAuthor);

        for (int i = 0 ; i < count; i ++) {
            Feed feedWithAuthorId = FeedFixture.createFeed(author, weather);
            Feed feedWithOtherAuthorId = FeedFixture.createFeed(otherAuthor, weather);
            em.persist(feedWithAuthorId);
            em.persist(feedWithOtherAuthorId);
        }

        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null, null, limit + 1, sortBy, sortDirection, null, null, null, author.getId()
        );

        // then
        assertThat(result).hasSize(count);
        assertThat(result).allSatisfy(feed -> {
            assertThat(feed.getAuthor().getId()).isEqualTo(author.getId());
        });
    }

    @Test
    void 모든_검색_조건을_일치하는_피드만_정렬하여_반환된다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;
        String keyword = "맑음";
        SkyStatus skyStatus = SkyStatus.CLEAR;
        Precipitation precipitation = Precipitation.NONE;
        UUID authorId = author.getId();

        Weather targetWeather = Weather.builder()
            .forecastAt(Instant.now())
            .forecastedAt(Instant.now())
            .location(location)
            .skyStatus(skyStatus)
            .precipitationType(precipitation)
            .build();
        em.persist(targetWeather);

        Feed targetFeed = FeedFixture.createFeedWithKeyword(author, targetWeather, keyword);
        em.persist(targetFeed);

        // 노이즈 데이터
        em.persist(FeedFixture.createFeedWithKeyword(author, targetWeather, "흐림"));
        em.persist(FeedFixture.createFeedWithKeyword(author, weather, keyword));

        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null,
            null,
            limit,
            sortBy,
            sortDirection,
            keyword,
            skyStatus,
            precipitation,
            authorId
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(targetFeed.getId());
    }

    @Test
    void 모든_검색_조건을_일치하는_피드의_개수를_반환한다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;
        String keyword = "맑음";
        SkyStatus skyStatus = SkyStatus.CLEAR;
        Precipitation precipitation = Precipitation.NONE;
        UUID authorId = author.getId();

        Weather targetWeather = Weather.builder()
            .forecastAt(Instant.now())
            .forecastedAt(Instant.now())
            .location(location)
            .skyStatus(skyStatus)
            .precipitationType(precipitation)
            .build();
        em.persist(targetWeather);

        Feed targetFeed = FeedFixture.createFeedWithKeyword(author, targetWeather, keyword);
        em.persist(targetFeed);

        // 노이즈 데이터
        em.persist(FeedFixture.createFeedWithKeyword(author, targetWeather, "흐림"));
        em.persist(FeedFixture.createFeedWithKeyword(author, weather, keyword));

        em.flush();
        em.clear();

        // when
        Long result = feedRepository.countByFilter(
            keyword,
            skyStatus,
            precipitation,
            authorId
        );

        // then
        assertThat(result).isEqualTo(1L);
    }

    @Test
    void 필터가_없을_때_전체_피드_개수를_반환한다() {

        // given
        int count = 10;
        for (int i = 0; i < count; i++) {
            em.persist(FeedFixture.createFeed(author, weather));
        }
        em.flush();
        em.clear();

        // when
        Long result = feedRepository.countByFilter(null, null, null, null);

        // then
        assertThat(result).isEqualTo(count);
    }

    @Test
    void 일치하는_피드가_없을_때_0을_반환한다() {

        // given
        em.persist(FeedFixture.createFeedWithKeyword(author, weather, "맑음"));
        em.flush();
        em.clear();

        // when
        Long result = feedRepository.countByFilter("흐림", null, null, null);

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    void 삭제된_피드는_조회_및_집계에서_제외된다() {

        // given
        Feed alive = FeedFixture.createFeed(author, weather);
        em.persist(alive);
        Feed deleted = FeedFixture.createFeed(author, weather);
        ReflectionTestUtils.setField(deleted, "isDeleted", true);
        em.persist(deleted);
        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(null, null, 10, "createdAt", SortDirection.DESCENDING, null, null, null, null);
        Long count = feedRepository.countByFilter(null, null, null, null);

        // then
        assertThat(result).extracting(Feed::getId)
            .contains(alive.getId())
            .doesNotContain(deleted.getId());
        assertThat(count).isEqualTo(1L);
    }
}
