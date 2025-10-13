package com.samsamotot.otboo.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.samsamotot.otboo.clothes.entity.Clothes;
import com.samsamotot.otboo.clothes.repository.ClothesRepository;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedCursorRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.dto.FeedUpdateRequest;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedClothes;
import com.samsamotot.otboo.feed.mapper.FeedMapper;
import com.samsamotot.otboo.feed.repository.FeedLikeRepository;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.feed.repository.FeedSearchRepository;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.notification.dto.event.FeedCreatedEvent;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import com.samsamotot.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Feed 서비스 단위 테스트")
public class FeedServiceTest {

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private FeedLikeRepository feedLikeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private FeedSearchRepository feedSearchRepository;

    @Mock
    private FeedMapper feedMapper;

    @Mock
    private FeedDataSyncService feedDataSyncService;

    @InjectMocks
    private FeedServiceImpl feedService;

    User mockUser;
    Weather mockWeather;

    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createUser();
        Location location = LocationFixture.createLocation();
        Grid grid = location.getGrid();
        mockWeather = WeatherFixture.createWeather(grid);
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(mockWeather, "id", UUID.randomUUID());
    }

    @Nested
    @DisplayName("피드 생성 테스트")
    class FeedCreateTest {

        @Test
        void 피드를_생성하면_FeedDto를_반환해야한다() {

            // given
            UUID userId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(clothesId);

            Clothes mockClothes = Clothes.builder().build();
            List<Clothes> mockClothesList = List.of(mockClothes);
            List<FeedClothes> mockFeedClothesList = List.of(FeedClothes.builder().build());

            String content = "피드 등록 테스트";

            FeedCreateRequest request = FeedCreateRequest.builder()
                .authorId(userId)
                .weatherId(weatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            Feed savedFeed = FeedFixture.createFeed(mockUser, mockWeather, mockFeedClothesList, content);
            ReflectionTestUtils.setField(savedFeed, "id", UUID.randomUUID());

            FeedDto expectedDto = FeedDto.builder()
                .id(savedFeed.getId())
                .content(savedFeed.getContent())
                .build();

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(mockUser));
            given(weatherRepository.findById(any(UUID.class))).willReturn(Optional.of(mockWeather));
            given(clothesRepository.findAllById(any())).willReturn(mockClothesList);
            given(feedRepository.save(any(Feed.class))).willReturn(savedFeed);
            given(feedMapper.toDto(savedFeed)).willReturn(expectedDto);

            // when
            FeedDto result = feedService.create(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(expectedDto.id());
            assertThat(result.content()).isEqualTo(expectedDto.content());
            verify(feedDataSyncService).syncFeedToElasticsearch(any(FeedDto.class));
            verify(eventPublisher).publishEvent(any(FeedCreatedEvent.class));
        }

        @Test
        void 피드를_등록할_때_존재하지_않는_유저면_예외가_발생한다() {

            // given
            UUID invalidUserId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(UUID.randomUUID());

            String content = "피드 등록 테스트";

            FeedCreateRequest request = FeedCreateRequest.builder()
                .authorId(invalidUserId)
                .weatherId(weatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.create(request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(weatherRepository, never()).findById(any());
            verify(clothesRepository, never()).findAllById(any());
            verify(feedRepository, never()).save(any());
        }

        @Test
        void 피드를_등록할_때_존재하지_않는_날씨면_예외가_발생한다() {

            // given
            UUID userId = UUID.randomUUID();
            UUID invalidWeatherId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(UUID.randomUUID());
            String content = "피드 등록 테스트";

            FeedCreateRequest request = FeedCreateRequest.builder()
                .authorId(userId)
                .weatherId(invalidWeatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(mockUser));
            given(weatherRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.create(request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.WEATHER_NOT_FOUND);

            verify(userRepository).findById(userId);
            verify(clothesRepository, never()).findAllById(any());
            verify(feedRepository, never()).save(any());
        }

        @Test
        void 피드를_등록할_때_존재하지_않는_의상이면_예외가_발생한다() {

            // given
            UUID userId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            List<UUID> invalidClothesIds = List.of(UUID.randomUUID());
            String content = "피드 등록 테스트";

            FeedCreateRequest request = FeedCreateRequest.builder()
                .authorId(userId)
                .weatherId(weatherId)
                .clothesIds(invalidClothesIds)
                .content(content)
                .build();

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(mockUser));
            given(weatherRepository.findById(any(UUID.class))).willReturn(Optional.of(mockWeather));
            given(clothesRepository.findAllById(any())).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> feedService.create(request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.CLOTHES_NOT_FOUND);

            verify(feedRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("피드 목록 조회 테스트")
    class FeedListGetTest {

        UUID defaultUserId = UUID.randomUUID();

        @Test
        void 커서없이_조회시_주어진_정렬조건으로_레포지토리를_호출한다() {

            // given
            FeedCursorRequest request = createDefaultRequest();

            given(feedSearchRepository.findByCursor(any(), any(), anyInt(), anyString(), any(SortDirection.class), any(), any(), any(), any()))
                .willReturn(new CursorResponse<>(List.of(), null, null, false, 0L, request.sortBy(), request.sortDirection()));

            // when
            feedService.getFeeds(request, defaultUserId);

            // then
            verify(feedSearchRepository).findByCursor(
                isNull(String.class),
                isNull(UUID.class),
                eq(request.limit() + 1),
                eq(request.sortBy()),
                eq(request.sortDirection()),
                isNull(String.class),
                isNull(SkyStatus.class),
                isNull(Precipitation.class),
                isNull(UUID.class)
            );
        }

        @Test
        void hasNext_true면_nextCursor를_생성한다() {

            // given
            int limit = 2;
            String sortBy = "createdAt";
            SortDirection sortDirection = SortDirection.DESCENDING;

            FeedCursorRequest request = FeedCursorRequest.builder()
                .limit(limit)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

            List<FeedDto> contents = new ArrayList<>();
            for (int i = 0; i < limit + 1; i++) {
                Feed feed = FeedFixture.createFeed(mockUser, mockWeather);
                ReflectionTestUtils.setField(feed, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(feed, "createdAt", Instant.now().plusSeconds(i));
                FeedDto feedDto = FeedFixture.createFeedDto(feed);
                contents.add(feedDto);
            }

            given(feedSearchRepository.findByCursor(
                any(), any(), anyInt(), anyString(), any(SortDirection.class), any(), any(), any(), any()
            )).willReturn(new CursorResponse<>(contents.subList(0, limit), contents.get(limit).createdAt().toString(), contents.get(limit).id(), true, 3L, request.sortBy(), request.sortDirection()));

            // when
            CursorResponse<FeedDto> result = feedService.getFeeds(request, defaultUserId);

            // then
            assertThat(result.data()).hasSize(limit);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();
            verify(feedSearchRepository).findByCursor(
                isNull(String.class),
                isNull(UUID.class),
                eq(request.limit() + 1),
                eq(request.sortBy()),
                eq(request.sortDirection()),
                isNull(String.class),
                isNull(SkyStatus.class),
                isNull(Precipitation.class),
                isNull(UUID.class)
            );
        }

        @Test
        void hasNext_false면_nextCursor는_null이다() {

            // given
            int limit = 2;
            String sortBy = "createdAt";
            SortDirection sortDirection = SortDirection.DESCENDING;

            FeedCursorRequest request = FeedCursorRequest.builder()
                .limit(limit)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
            Feed a = FeedFixture.createFeed(mockUser, mockWeather);
            List<FeedDto> contents = List.of(FeedFixture.createFeedDto(a));

            given(feedSearchRepository.findByCursor(
                any(), any(), anyInt(), anyString(), any(SortDirection.class), any(), any(), any(), any()
            )).willReturn(new CursorResponse<>(contents, null, null, false, 1L, request.sortBy(), request.sortDirection()));

            // when
            CursorResponse<FeedDto> result = feedService.getFeeds(request, defaultUserId);

            // then
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            verify(feedSearchRepository).findByCursor(
                isNull(String.class),
                isNull(UUID.class),
                eq(request.limit() + 1),
                eq(request.sortBy()),
                eq(request.sortDirection()),
                isNull(String.class),
                isNull(SkyStatus.class),
                isNull(Precipitation.class),
                isNull(UUID.class)
            );
        }

        @Test
        void 생성된_커서정보는_마지막_조회결과의_정보를_올바르게_포함한다() {

            // given
            FeedCursorRequest request = createDefaultRequest();
            List<FeedDto> contents = new ArrayList<>();
            for (int i = 0; i < request.limit() + 1; i++) {
                Feed feed = FeedFixture.createFeed(mockUser, mockWeather);
                ReflectionTestUtils.setField(feed, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(feed, "createdAt", Instant.now().plusSeconds(i));
                FeedDto feedDto = FeedFixture.createFeedDto(feed);
                contents.add(feedDto);
            }
            FeedDto lastFeedDto = contents.get(request.limit() - 1);

            given(feedSearchRepository.findByCursor(
                any(), any(), anyInt(), anyString(), any(SortDirection.class), any(), any(), any(), any()
            )).willReturn(new CursorResponse<>(contents.subList(0, request.limit()), lastFeedDto.createdAt().toString(), lastFeedDto.id(), true, 11L, request.sortBy(), request.sortDirection()));

            // when
            CursorResponse<FeedDto> result = feedService.getFeeds(request, defaultUserId);

            // then
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(lastFeedDto.createdAt().toString());
            assertThat(result.nextIdAfter()).isEqualTo(lastFeedDto.id());

            verify(feedSearchRepository).findByCursor(
                isNull(String.class),
                isNull(UUID.class),
                eq(request.limit() + 1),
                eq(request.sortBy()),
                eq(request.sortDirection()),
                isNull(String.class),
                isNull(SkyStatus.class),
                isNull(Precipitation.class),
                isNull(UUID.class)
            );
        }

        @Test
        void 조회시_필터링된_총_개수를_함께_반환한다() {

            // given
            int limit = 10;
            String sortBy = "createdAt";
            SortDirection sortDirection = SortDirection.DESCENDING;
            long expectedTotalCount = 1L;
            String keyword = "keyword";

            Feed f1 = FeedFixture.createFeedWithKeyword(mockUser, mockWeather, keyword);
            ReflectionTestUtils.setField(f1, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(f1, "createdAt", Instant.now());
            List<FeedDto> contents = List.of(FeedFixture.createFeedDto(f1));

            FeedCursorRequest request = FeedCursorRequest.builder()
                .limit(limit)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .keywordLike(keyword)
                .build();

            given(feedSearchRepository.findByCursor(
                any(), any(), anyInt(), anyString(), any(SortDirection.class), any(), any(), any(), any()
            )).willReturn(new CursorResponse<>(contents, null, null, false, expectedTotalCount, request.sortBy(), request.sortDirection()));

            // when
            CursorResponse<FeedDto> result = feedService.getFeeds(request, defaultUserId);

            // then
            assertThat(result.totalCount()).isEqualTo(expectedTotalCount);

            verify(feedSearchRepository).findByCursor(
                eq(request.cursor()),
                eq(request.idAfter()),
                eq(request.limit() + 1),
                eq(request.sortBy()),
                eq(request.sortDirection()),
                eq(request.keywordLike()),
                eq(request.skyStatusEqual()),
                eq(request.precipitationTypeEqual()),
                eq(request.authorIdEqual())
            );
        }

        @Test
        void 커서_포맷이_잘못되면_예외가_발생한다() {

            // given
            String badCursor = "Invalid Cursor";
            int limit = 2;
            String sortBy = "createdAt";
            SortDirection sortDirection = SortDirection.DESCENDING;

            FeedCursorRequest request = FeedCursorRequest.builder()
                .cursor(badCursor)
                .limit(limit)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

            // when & then
            assertThatThrownBy(() -> feedService.getFeeds(request, defaultUserId))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT);

            verifyNoInteractions(feedSearchRepository);
        }

        @Test
        void 조회할_피드가_전혀_없을_때_빈_리스트와_null_커서를_반환한다() {

            // given
            FeedCursorRequest request = createDefaultRequest();

            given(feedSearchRepository.findByCursor(
                any(), any(), anyInt(), anyString(), any(SortDirection.class), any(), any(), any(), any()
            )).willReturn(new CursorResponse<>(List.of(), null, null, false, 0L, request.sortBy(), request.sortDirection()));

            // when
            CursorResponse<FeedDto> result = feedService.getFeeds(request, defaultUserId);

            // then
            assertThat(result.data()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        void 정렬_조건과_검색_조건을_지정하면_레포지토리에_정확하게_전달한다() {

            // given
            int limit = 10;
            String sortBy = "createdAt";
            SortDirection sortDirection = SortDirection.DESCENDING;
            String keyword = "a";
            SkyStatus skyStatus = SkyStatus.CLEAR;
            Precipitation precipitation = Precipitation.NONE;
            UUID authorId = UUID.randomUUID();

            FeedCursorRequest request = FeedCursorRequest.builder()
                .cursor(null)
                .idAfter(null)
                .limit(limit)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .keywordLike(keyword)
                .skyStatusEqual(skyStatus)
                .precipitationTypeEqual(precipitation)
                .authorIdEqual(authorId)
                .build();

            given(feedSearchRepository.findByCursor(any(), any(), anyInt(), anyString(),
                any(SortDirection.class), any(), any(), any(), any()))
                .willReturn(new CursorResponse<>(List.of(), null, null, false, 0L, sortBy, sortDirection));

            // when
            feedService.getFeeds(request, defaultUserId);

            // then
            verify(feedSearchRepository).findByCursor(
                isNull(String.class),
                isNull(UUID.class),
                eq(request.limit() + 1),
                eq(request.sortBy()),
                eq(request.sortDirection()),
                eq(request.keywordLike()),
                eq(request.skyStatusEqual()),
                eq(request.precipitationTypeEqual()),
                eq(request.authorIdEqual())
            );
        }

        // 헬퍼 메서드
        private FeedCursorRequest createDefaultRequest() {

            int limit = 10;
            String sortBy = "createdAt";
            SortDirection sortDirection = SortDirection.DESCENDING;

            return FeedCursorRequest.builder()
                .cursor(null)
                .idAfter(null)
                .limit(limit)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .keywordLike(null)
                .skyStatusEqual(null)
                .precipitationTypeEqual(null)
                .authorIdEqual(null)
                .build();
        }
    }

    @Nested
    @DisplayName("피드 수정 테스트")
    class FeedUpdateTest {

        @Test
        void 피드를_수정하면_수정된_FeedDto를_반환해야_한다() {

            // given
            UUID feedId = UUID.randomUUID();
            Feed originFeed = FeedFixture.createFeed(mockUser, mockWeather);
            ReflectionTestUtils.setField(originFeed, "id", feedId);
            UUID authorId = mockUser.getId();
            String updateContent = "피드 수정 테스트";

            FeedUpdateRequest request = FeedUpdateRequest.builder()
                .content(updateContent)
                .build();

            FeedDto expectedFeedDto = FeedFixture.createFeedDtoWithContent(originFeed, updateContent);

            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.of(originFeed));
            given(feedMapper.toDto(any(Feed.class))).willReturn(expectedFeedDto);

            // when
            FeedDto result = feedService.update(feedId, authorId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(feedId);
            assertThat(result.content()).isEqualTo(updateContent);
            assertThat(result.author().userId()).isEqualTo(authorId);
            verify(feedDataSyncService).syncFeedToElasticsearch(any(FeedDto.class));
        }

        @Test
        void 존재하지_않는_피드라면_예외가_발생한다() {

            // given
            UUID invalidFeedId = UUID.randomUUID();
            UUID userId = mockUser.getId();
            String updateContent = "피드 수정 테스트";

            FeedUpdateRequest request = FeedUpdateRequest.builder()
                .content(updateContent)
                .build();

            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                feedService.update(invalidFeedId, userId, request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_NOT_FOUND);
        }

        @Test
        void 피드를_수정할_때_작성자가_아니라면_예외가_발생한다() {

            // given
            UUID feedId = UUID.randomUUID();
            UUID invalidAuthorId = UUID.randomUUID();
            String updateContent = "피드 수정 테스트";

            FeedUpdateRequest request = FeedUpdateRequest.builder()
                .content(updateContent)
                .build();

            Feed feed = FeedFixture.createFeed(mockUser, mockWeather);
            ReflectionTestUtils.setField(feed, "id", feedId);

            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.of(feed));

            // when & then
            assertThatThrownBy(() -> feedService.update(feedId, invalidAuthorId, request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_FEED_MODIFICATION);
        }
    }

    @Nested
    @DisplayName("피드 논리 삭제 테스트")
    class FeedSoftDeleteTest {

        @Test
        void 피드를_논리삭제_하면_isDeleted가_true가_된다() {

            // given
            UUID feedId = UUID.randomUUID();
            Feed feed = FeedFixture.createFeed(mockUser, mockWeather);
            ReflectionTestUtils.setField(feed, "id", feedId);
            UUID userId = mockUser.getId();

            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.of(feed));

            // when
            Feed result = feedService.delete(feedId, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.isDeleted()).isTrue();
            verify(feedDataSyncService).softDeleteFeedFromElasticsearch(feedId);
        }

        @Test
        void 존재하지_않는_피드_삭제_요청시_예외가_발생한다() {

            // given
            UUID invalidFeedId = UUID.randomUUID();
            UUID userId = mockUser.getId();

            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.delete(invalidFeedId, userId))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_NOT_FOUND);
        }

        @Test
        void 이미_삭제_처리된_피드_삭제_요청시_예외가_발생한다() {

            // given
            UUID feedId = UUID.randomUUID();
            Feed feed = Feed.builder()
                .author(mockUser)
                .weather(mockWeather)
                .isDeleted(true)
                .build();
            ReflectionTestUtils.setField(feed, "id", feedId);
            UUID userId = mockUser.getId();

            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.delete(feedId, userId))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_NOT_FOUND);
        }

        @Test
        void 작성자가_아니라면_예외가_발생한다() {

            // given
            UUID feedId = UUID.randomUUID();
            Feed feed = FeedFixture.createFeed(mockUser, mockWeather);
            ReflectionTestUtils.setField(feed, "id", feedId);
            UUID otherUserId = UUID.randomUUID();

            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.of(feed));

            // when & then
            assertThatThrownBy(() -> feedService.delete(feedId, otherUserId))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_FEED_DELETION);
        }
    }
}
