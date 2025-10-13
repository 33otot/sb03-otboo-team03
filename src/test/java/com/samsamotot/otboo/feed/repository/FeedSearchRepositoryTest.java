package com.samsamotot.otboo.feed.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.samsamotot.otboo.common.config.ElasticsearchTestConfig;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.document.FeedDocument;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.mapper.FeedDocumentMapper;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataElasticsearchTest
@Import(ElasticsearchTestConfig.class)
@DisplayName("FeedSearch 레포지토리 슬라이스 테스트")
class FeedSearchRepositoryTest {

    @Container
    static ElasticsearchContainer es =
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.14.3")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false");

    @DynamicPropertySource
    static void esProps(DynamicPropertyRegistry reg) {
        reg.add("spring.elasticsearch.host", () -> es.getHost());
        reg.add("spring.elasticsearch.port", () -> es.getMappedPort(9200));
    }

    @Autowired
    ElasticsearchOperations operations;

    @Autowired
    FeedSearchRepository feedSearchRepository;

    @MockitoBean
    FeedDocumentMapper feedDocumentMapper;

    Instant baseTime = Instant.now().minusSeconds(3000);
    User mockUser;
    Grid mockGrid;
    Weather mockWeather;

    @BeforeEach
    void initAndBootstrap() {
        // 도메인 준비
        mockUser = UserFixture.createUser();
        Location location = LocationFixture.createLocation();
        mockGrid = location.getGrid();
        mockWeather = WeatherFixture.createWeather(mockGrid);
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(mockWeather, "id", UUID.randomUUID());

        // 인덱스 준비
        IndexOperations indexOps = operations.indexOps(FeedDocument.class);
        if (indexOps.exists()) indexOps.delete();

        var settings = Document.from(indexOps.createSettings());
        if (settings != null && !settings.isEmpty()) indexOps.create(settings);
        else indexOps.create();

        var mapping = indexOps.createMapping(FeedDocument.class);
        if (mapping != null && !mapping.isEmpty()) indexOps.putMapping(mapping);

        // 시드 + refresh
        seed();
        indexOps.refresh();
    }

    @BeforeEach
    void stubMapper() {
        Mockito.lenient().when(feedDocumentMapper.toDto(Mockito.any())).thenAnswer(inv -> {
            FeedDocument d = inv.getArgument(0);
            return FeedDto.builder()
                .id(d.id())
                .author(d.author())
                .weather(d.weather())
                .content(d.content())
                .likeCount(d.likeCount())
                .commentCount(d.commentCount())
                .likedByMe(d.likedByMe())
                .createdAt(d.createdAt())
                .updatedAt(d.updatedAt())
                .build();
        });
    }

    private void seed() {
        // 정상 문서 4건
        for (int i = 0; i < 5; i++) {
            Feed feed = FeedFixture.createFeedWithCreatedAt(mockUser, mockWeather, baseTime.plusSeconds(i * 1000));
            ReflectionTestUtils.setField(feed, "id", UUID.randomUUID());
            FeedDocument doc = FeedFixture.createFeedDocument(feed);
            operations.save(doc);
        }

        // 삭제 문서
        Feed deletedFeed = FeedFixture.createFeedWithCreatedAt(mockUser, mockWeather, baseTime.plusSeconds(10000));
        ReflectionTestUtils.setField(deletedFeed, "id", UUID.randomUUID());
        FeedDocument deletedDoc = FeedFixture.createFeedDocumentWithSoftDeleted(deletedFeed);
        operations.save(deletedDoc);
    }

    @Test
    void createdAt_내림차순으로_피드를_조회한다() {

        CursorResponse<FeedDto> page1 = feedSearchRepository.findByCursor(
            null,
            null,
            3,
            "createdAt",
            SortDirection.DESCENDING,
            null,
            null,
            null,
            null
        );

        assertThat(page1.data()).hasSize(3);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.nextCursor()).isNotBlank();
        assertThat(page1.nextIdAfter()).isNotNull();

        // 다음 페이지
        CursorResponse<FeedDto> page2 = feedSearchRepository.findByCursor(
            page1.nextCursor(),
            page1.nextIdAfter(),
            3,
            "createdAt",
            SortDirection.DESCENDING,
            null, null, null, null
        );

        // 남은 2건(총 5건 중 앞에서 3건 소비)
        assertThat(page2.data()).hasSize(2);
        assertThat(page2.data()).allMatch(d -> !"삭제된 피드".equals(d.content())); // 삭제문서 제외 검증
        assertThat(page2.hasNext()).isFalse();
        assertThat(page2.nextCursor()).isNull();
        assertThat(page2.nextIdAfter()).isNull();
    }

    @Test
    void 키워드_조건으로_검색시_해당_키워드가_포함된_피드만_반환된다() {

        // 조건 검증용 문서
        Feed target = FeedFixture.createFeed(mockUser, mockWeather);
        ReflectionTestUtils.setField(target, "id", UUID.randomUUID());
        FeedDocument targetDoc = FeedFixture.createfeedDocumentWithContent(target, "검색전용12345");
        IndexOperations indexOps = operations.indexOps(FeedDocument.class);
        operations.save(targetDoc);
        indexOps.refresh();

        CursorResponse<FeedDto> res = feedSearchRepository.findByCursor(
            null,
            null,
            10,
            "likeCount",
            SortDirection.ASCENDING,
            "검색전용12345",
            null,
            null,
            null
        );
        assertThat(res.data()).extracting(FeedDto::content).containsExactly("검색전용12345");
        assertThat(res.totalCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 하늘상태_필터_적용시_지정_상태의_피드만_반환된다() {

        // 조건 검증용 문서
        Weather weather = WeatherFixture.createWeatherWithSkyStatus(mockGrid, SkyStatus.MOSTLY_CLOUDY);
        Feed target = FeedFixture.createFeed(mockUser, weather);
        ReflectionTestUtils.setField(target, "id", UUID.randomUUID());
        FeedDocument targetDoc = FeedFixture.createFeedDocument(target);
        IndexOperations indexOps = operations.indexOps(FeedDocument.class);
        operations.save(targetDoc);
        indexOps.refresh();

        CursorResponse<FeedDto> res = feedSearchRepository.findByCursor(
            null,
            null,
            10,
            "likeCount",
            SortDirection.ASCENDING,
            null,
            SkyStatus.MOSTLY_CLOUDY,
            null,
            null
        );
        assertThat(res.data()).extracting(
            feedDto -> feedDto.weather().skyStatus()).containsExactly(SkyStatus.MOSTLY_CLOUDY);
        assertThat(res.totalCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 강수유형_필터_적용시_지정_유형의_피드만_반환된다() {
        // 조건 검증용 문서
        Weather weather = WeatherFixture.createWeatherWithPrecipitation(mockGrid, Precipitation.RAIN);
        Feed target = FeedFixture.createFeed(mockUser, weather);
        ReflectionTestUtils.setField(target, "id", UUID.randomUUID());
        FeedDocument targetDoc = FeedFixture.createFeedDocumentWithWeather(target);
        IndexOperations indexOps = operations.indexOps(FeedDocument.class);
        operations.save(targetDoc);
        indexOps.refresh();

        CursorResponse<FeedDto> res = feedSearchRepository.findByCursor(
            null,
            null,
            10,
            "likeCount",
            SortDirection.ASCENDING,
            null,
            null,
            Precipitation.RAIN,
            null
        );
        assertThat(res.data()).extracting(
            feedDto -> feedDto.weather().precipitation().type()).containsExactly(Precipitation.RAIN);
        assertThat(res.totalCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 작성자ID_필터_적용시_해당_작성자의_피드만_반환된다() {
        // 조건 검증용 문서
        UUID authorId = UUID.randomUUID();
        User author = UserFixture.createUser();
        ReflectionTestUtils.setField(author, "id", authorId);
        Feed target = FeedFixture.createFeed(author, mockWeather);
        ReflectionTestUtils.setField(target, "id", UUID.randomUUID());
        FeedDocument targetDoc = FeedFixture.createFeedDocument(target);
        IndexOperations indexOps = operations.indexOps(FeedDocument.class);
        operations.save(targetDoc);
        indexOps.refresh();

        CursorResponse<FeedDto> res = feedSearchRepository.findByCursor(
            null,
            null,
            10,
            "likeCount",
            SortDirection.ASCENDING,
            null,
            null,
            null,
            authorId
        );
        assertThat(res.data()).extracting(
            feedDto -> feedDto.author().userId()).containsExactly(authorId);
        assertThat(res.totalCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 잘못된_커서_형식이면_예외() {
        // createdAt 정렬인데 cursor에 ISO가 아닌 임의 문자열
        assertThrows(OtbooException.class, () ->
            feedSearchRepository.findByCursor(
                "NOT_ISO_TIME",
                UUID.randomUUID(),
                3,
                "createdAt",
                SortDirection.DESCENDING,
                null,
                null,
                null,
                null
            )
        );
    }

    @Test
    void 매칭된게_없으면_빈_리스트를_반환한다() {
        CursorResponse<FeedDto> res = feedSearchRepository.findByCursor(
            null,
            null,
            10,
            "createdAt",
            SortDirection.DESCENDING,
            "존재하지 않는 키워드 테스트",
            null,
            null,
            null
        );
        assertThat(res.data()).isEmpty();
        assertThat(res.totalCount()).isZero();
        assertThat(res.hasNext()).isFalse();
    }
}
