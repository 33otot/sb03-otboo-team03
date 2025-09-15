package com.samsamotot.otboo.feed.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samsamotot.otboo.common.config.QueryDslConfig;
import com.samsamotot.otboo.common.config.TestJpaAuditingConfig;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestJpaAuditingConfig.class, QueryDslConfig.class})
@DisplayName("Feed 레포지토리 슬라이스 테스트")
public class FeedRepositoryTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private TestEntityManager em;

    Instant baseTime = Instant.now().minusSeconds(3000);

    @Test
    void createdAt_내림차순으로_피드를_조회한다() {

        // given
        int limit = 5;
        String sortBy = "createdAt";
        SortDirection sortDirection = SortDirection.DESCENDING;

        for (int i = 0; i < 5; i ++) {
            Feed feed = FeedFixture.createFeedWithCreatedAt(baseTime.plusSeconds(i * 1000));
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
            Feed feed = FeedFixture.createFeedWithCreatedAt(baseTime.plusSeconds(i * 1000));
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
            Feed feed = FeedFixture.createFeedWithLikeCount((long) i);
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
            Feed feed = FeedFixture.createFeedWithLikeCount((long) i);
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
            Feed feed = FeedFixture.createFeedWithLikeCount((long) i);
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
            Feed feedWithKeyword = FeedFixture.createFeedWithKeyword(keyword + i);
            Feed feedWithOtherKeyword = FeedFixture.createFeedWithKeyword("계절" + i);
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

        for (int i = 0 ; i < count; i ++) {
            Feed feedWithSkyStatus = FeedFixture.createFeedWithSkyStatus(skyStatus);
            Feed feedWithOtherSkyStatus = FeedFixture.createFeedWithSkyStatus(SkyStatus.CLOUDY);
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

        for (int i = 0 ; i < count; i ++) {
            Feed feedWithPrecipitation = FeedFixture.createFeedWithPrecipitation(precipitation);
            Feed feedWithOtherPrecipitation = FeedFixture.createFeedWithPrecipitation(Precipitation.NONE);
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
        UUID authorId = UUID.randomUUID();
        int count = 5;

        for (int i = 0 ; i < count; i ++) {
            Feed feedWithAuthorId = FeedFixture.createFeedWithAuthorId(authorId);
            Feed feedWithOtherAuthorId = FeedFixture.createFeedWithAuthorId(UUID.randomUUID());
            em.persist(feedWithAuthorId);
            em.persist(feedWithOtherAuthorId);
        }

        em.flush();
        em.clear();

        // when
        List<Feed> result = feedRepository.findByCursor(
            null, null, limit + 1, sortBy, sortDirection, null, null, null, authorId
        );

        // then
        assertThat(result).hasSize(count);
        assertThat(result).allSatisfy(feed -> {
            assertThat(feed.getAuthor().getId()).isEqualTo(authorId);
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
        UUID authorId = UUID.randomUUID();

        Feed targetFeed = FeedFixture.createFeedWithAllConditions(keyword, skyStatus, precipitation, authorId);
        em.persist(targetFeed);

        // 노이즈 데이터
        em.persist(FeedFixture.createFeedWithAllConditions("흐림", skyStatus, precipitation, authorId));
        em.persist(FeedFixture.createFeedWithAllConditions(keyword, SkyStatus.CLOUDY, precipitation, authorId));
        em.persist(FeedFixture.createFeedWithAllConditions(keyword, skyStatus, Precipitation.RAIN, authorId));
        em.persist(FeedFixture.createFeedWithAllConditions(keyword, skyStatus, precipitation, UUID.randomUUID()));

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
        String keyword = "맑음";
        SkyStatus skyStatus = SkyStatus.CLEAR;
        Precipitation precipitation = Precipitation.NONE;
        UUID authorId = UUID.randomUUID();

        Feed targetFeed = FeedFixture.createFeedWithAllConditions(keyword, skyStatus, precipitation, authorId);
        em.persist(targetFeed);

        // 노이즈 데이터
        em.persist(FeedFixture.createFeedWithAllConditions("흐림", skyStatus, precipitation, authorId));
        em.persist(FeedFixture.createFeedWithAllConditions(keyword, SkyStatus.CLOUDY, precipitation, authorId));
        em.persist(FeedFixture.createFeedWithAllConditions(keyword, skyStatus, Precipitation.RAIN, authorId));
        em.persist(FeedFixture.createFeedWithAllConditions(keyword, skyStatus, precipitation, UUID.randomUUID()));

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
            em.persist(FeedFixture.createDefaultFeed());
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
        em.persist(FeedFixture.createFeedWithKeyword("맑음"));
        em.flush();
        em.clear();

        // when
        Long result = feedRepository.countByFilter("흐림", null, null, null);

        // then
        assertThat(result).isEqualTo(0L);
    }
}
