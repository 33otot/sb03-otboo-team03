package com.samsamotot.otboo.feed.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.clothes.dto.OotdDto;
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
import com.samsamotot.otboo.feed.service.FeedService;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import com.samsamotot.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(FeedController.class)
@DisplayName("Feed 컨트롤러 슬라이스 테스트")
public class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedService feedService;

    @Nested
    @DisplayName("피드 생성 테스트")
    class FeedCreateTest {

        @Test
        void 유효한_등록_요청이면_등록_성공후_201과_DTO가_반환되어야_한다() throws Exception {

            // given
            String content = "피드 생성 테스트";
            UUID authorId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(clothesId);

            FeedCreateRequest feedCreateRequest = FeedCreateRequest.builder()
                .authorId(authorId)
                .weatherId(weatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            AuthorDto authorDto = AuthorDto.builder()
                .userId(authorId)
                .build();
            WeatherDto weatherDto = WeatherDto.builder()
                .id(weatherId)
                .build();
            OotdDto ootdDto = OotdDto.builder()
                .clothesId(clothesId)
                .build();
            FeedDto feedDto = FeedFixture.createFeedDto(content, authorDto, weatherDto, List.of(ootdDto));

            given(feedService.create(any(FeedCreateRequest.class))).willReturn(feedDto);

            // when & then
            mockMvc.perform(post("/api/feeds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(feedCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.userId").value(authorId.toString()))
                .andExpect(jsonPath("$.weather.id").value(weatherId.toString()))
                .andExpect(jsonPath("$.ootds", hasSize(1)))
                .andExpect(jsonPath("$.ootds[0].clothesId").value(clothesId.toString()))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false));
        }

        @Test
        void 존재하지_않는_유저면_404가_반환되어야_한다() throws Exception {

            // given
            String content = "피드 생성 테스트";
            UUID invalidAuthorId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(clothesId);

            FeedCreateRequest feedCreateRequest = FeedCreateRequest.builder()
                .authorId(invalidAuthorId)
                .weatherId(weatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            given(feedService.create(any(FeedCreateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(feedCreateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.USER_NOT_FOUND.toString()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        }

        @Test
        void 존재하지_않는_날씨면_404가_반환되어야_한다() throws Exception {

            // given
            String content = "피드 생성 테스트";
            UUID authorId = UUID.randomUUID();
            UUID invalidWeatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();
            List<UUID> clothesIds = List.of(clothesId);

            FeedCreateRequest feedCreateRequest = FeedCreateRequest.builder()
                .authorId(authorId)
                .weatherId(invalidWeatherId)
                .clothesIds(clothesIds)
                .content(content)
                .build();

            given(feedService.create(any(FeedCreateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.WEATHER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(feedCreateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.WEATHER_NOT_FOUND.toString()))
                .andExpect(jsonPath("$.message").value(ErrorCode.WEATHER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.WEATHER_NOT_FOUND.getCode()));
        }

        @Test
        void 존재하지_않는_의상이면_404가_반환되어야_한다() throws Exception {

            // given
            String content = "피드 생성 테스트";
            UUID authorId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID invalidClothesId = UUID.randomUUID();
            List<UUID> invalidClothesIds = List.of(invalidClothesId);

            FeedCreateRequest feedCreateRequest = FeedCreateRequest.builder()
                .authorId(authorId)
                .weatherId(weatherId)
                .clothesIds(invalidClothesIds)
                .content(content)
                .build();

            given(feedService.create(any(FeedCreateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.CLOTHES_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(feedCreateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.CLOTHES_NOT_FOUND.toString()))
                .andExpect(jsonPath("$.message").value(ErrorCode.CLOTHES_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.CLOTHES_NOT_FOUND.getCode()));
        }

        @Test
        void 본문_길이가_1000자를_초과하면_400이_반환되어야_한다() throws Exception {

            // given
            String over = "a".repeat(1001); // 1001자
            FeedCreateRequest req = FeedCreateRequest.builder()
                .authorId(UUID.randomUUID())
                .weatherId(UUID.randomUUID())
                .clothesIds(List.of(UUID.randomUUID()))
                .content(over)
                .build();

            // when & then
            mockMvc.perform(post("/api/feeds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(feedService);
        }
    }

    @Nested
    @DisplayName("피드 목록 조회 테스트")
    class FeedListGetTest {

        @Test
        void 요청_조건대로_정렬된_피드목록과_200이_반환된다() throws Exception {

            // given
            Instant base = Instant.parse("2025-09-16T10:00:00Z");
            FeedDto oldFeed = FeedFixture.createFeedDtoWithCreatedAt("content1", null, null, List.of(), base.minusSeconds(1000));
            FeedDto newFeed = FeedFixture.createFeedDtoWithCreatedAt("content2", null, null, List.of(), base);
            List<FeedDto> feedDtos = List.of(newFeed, oldFeed);

            CursorResponse<FeedDto> expected = new CursorResponse<>(
                feedDtos,
                null,
                null,
                false,
                2L,
                "createdAt",
                SortDirection.DESCENDING
            );

            given(feedService.getFeeds(any(FeedCursorRequest.class), any(UUID.class)))
                .willReturn(expected);

            // when & then
            mockMvc.perform(get("/api/feeds")
                    .param("limit", String.valueOf(10))
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.toString())
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].content").value("content2"))
                .andExpect(jsonPath("$.data[1].content").value("content1"))
                .andExpect(jsonPath("$.totalCount").value(2L));
        }

        @Test
        void 커서_조건이_적용된_피드목록과_200이_반환된다() throws Exception {

            // given
            Instant base = Instant.parse("2025-09-16T10:00:00Z");
            FeedDto cursorFeed = FeedFixture.createFeedDtoWithCreatedAt("cursor", null, null, List.of(), base);
            FeedDto newFeed1 = FeedFixture.createFeedDtoWithCreatedAt("content3", null, null, List.of(), base.plusSeconds(1000));
            FeedDto newFeed2 = FeedFixture.createFeedDtoWithCreatedAt("content4", null, null, List.of(), base.plusSeconds(2000));
            List<FeedDto> feedDtos = List.of(newFeed1, newFeed2);

            String cursor = cursorFeed.createdAt().toString();
            UUID idAfter = cursorFeed.id();

            CursorResponse<FeedDto> expected = new CursorResponse<>(
                feedDtos,
                null,
                null,
                false,
                2L,
                "createdAt",
                SortDirection.DESCENDING
            );

            given(feedService.getFeeds(any(FeedCursorRequest.class), any(UUID.class)))
                .willReturn(expected);

            ArgumentCaptor<FeedCursorRequest> captor = ArgumentCaptor.forClass(FeedCursorRequest.class);

            // when & then
            mockMvc.perform(get("/api/feeds")
                    .param("cursor", cursor)
                    .param("idAfter", idAfter.toString())
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name())
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].content").value("content3"))
                .andExpect(jsonPath("$.data[1].content").value("content4"))
                .andExpect(jsonPath("$.totalCount").value(2L));

            verify(feedService).getFeeds(captor.capture(), any(UUID.class));
            FeedCursorRequest actual = captor.getValue();
            assertThat(actual.cursor()).isEqualTo(cursor);
            assertThat(actual.idAfter()).isEqualTo(idAfter);
            assertThat(actual.limit()).isEqualTo(10);
            assertThat(actual.sortBy()).isEqualTo("createdAt");
            assertThat(actual.sortDirection()).isEqualTo(SortDirection.DESCENDING);
        }

        @Test
        void 검색_조건이_적용된_피드목록과_200이_반환된다() throws Exception {

            // given
            UUID authorId = UUID.randomUUID();
            String keyword = "맑음";
            SkyStatus skyStatus = SkyStatus.CLEAR;
            Precipitation precipitation = Precipitation.NONE;

            User author = User.builder()
                .email("testUser@test.com")
                .password("testUser1234!")
                .username("testUser")
                .build();
            ReflectionTestUtils.setField(author, "id", authorId);
            ReflectionTestUtils.setField(author, "createdAt", Instant.now());

            Weather weather = Weather.builder()
                .forecastAt(Instant.now())
                .forecastedAt(Instant.now())
                .grid(LocationFixture.createLocation().getGrid())
                .skyStatus(skyStatus)
                .precipitationType(precipitation)
                .build();
            ReflectionTestUtils.setField(weather, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(weather, "createdAt", Instant.now());

            Feed feed = FeedFixture.createFeedWithKeyword(author, weather, keyword);
            FeedDto feedDto = FeedFixture.createFeedDto(feed);

            CursorResponse<FeedDto> expected = new CursorResponse<>(
                List.of(feedDto),
                null,
                null,
                false,
                1L,
                "createdAt",
                SortDirection.DESCENDING
            );

            given(feedService.getFeeds(any(FeedCursorRequest.class), any(UUID.class)))
                .willReturn(expected);

            ArgumentCaptor<FeedCursorRequest> captor = ArgumentCaptor.forClass(FeedCursorRequest.class);

            // when & then
            mockMvc.perform(get("/api/feeds")
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name())
                    .param("keywordLike", keyword)
                    .param("skyStatusEqual", skyStatus.name())
                    .param("precipitationTypeEqual", precipitation.name())
                    .param("authorIdEqual", authorId.toString())
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content").value(keyword))
                .andExpect(jsonPath("$.totalCount").value(1L));

            verify(feedService).getFeeds(captor.capture(), any(UUID.class));
            FeedCursorRequest actual = captor.getValue();
            assertThat(actual.limit()).isEqualTo(10);
            assertThat(actual.sortBy()).isEqualTo("createdAt");
            assertThat(actual.sortDirection()).isEqualTo(SortDirection.DESCENDING);
            assertThat(actual.keywordLike()).isEqualTo(keyword);
            assertThat(actual.skyStatusEqual()).isEqualTo(skyStatus);
            assertThat(actual.precipitationTypeEqual()).isEqualTo(precipitation);
            assertThat(actual.authorIdEqual()).isEqualTo(authorId);
        }

        @Test
        void 검색_결과가_없으면_빈_목록이_반환된다() throws Exception {

            // given
            CursorResponse<FeedDto> empty = new CursorResponse<>(
                List.of(), null, null, false, 0L, "createdAt", SortDirection.DESCENDING
            );
            given(feedService.getFeeds(any(FeedCursorRequest.class), any(UUID.class)))
                .willReturn(empty);

            // when & then
            mockMvc.perform(get("/api/feeds")
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name())
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0L))
                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        void limit_조건이_없으면_400이_반환된다() throws Exception {

            mockMvc.perform(get("/api/feeds")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DESCENDING")
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isBadRequest());
        }

        @Test
        void sortBy_조건이_없으면_400이_반환된다() throws Exception {

            mockMvc.perform(get("/api/feeds")
                    .param("limit", "10")
                    .param("sortDirection", "DESCENDING")
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isBadRequest());
        }

        @Test
        void sortDirection_조건이_없으면_400이_반환된다() throws Exception {

            mockMvc.perform(get("/api/feeds")
                    .param("limit", "10")
                    .param("sortBy", "createdAt"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void 잘못된_정렬기준이면_400이_반환된다() throws Exception {

            // given
            given(feedService.getFeeds(any(FeedCursorRequest.class), any(UUID.class)))
                .willThrow(new OtbooException(ErrorCode.INVALID_SORT_FIELD));

            // when & then
            mockMvc.perform(get("/api/feeds")
                    .param("limit", "10")
                    .param("sortBy", "invalidSortBy")
                    .param("sortDirection", SortDirection.DESCENDING.name())
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isBadRequest());
        }

        @Test
        void 잘못된_커서포맷이면_400이_반환된다() throws Exception {

            // given
            given(feedService.getFeeds(any(FeedCursorRequest.class), any(UUID.class)))
                .willThrow(new OtbooException(ErrorCode.INVALID_CURSOR_FORMAT));

            // when & then
            mockMvc.perform(get("/api/feeds")
                    .param("cursor", "invalidCursor")
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name())
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isBadRequest());
        }

        @Test
        void limit가_음수이면_400이_반환된다() throws Exception {

            mockMvc.perform(get("/api/feeds")
                    .param("limit", "-1")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", SortDirection.DESCENDING.name())
                    .param("userId", UUID.randomUUID().toString())
                )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("피드 수정 테스트")
    class FeedUpdateTest {

        @Test
        void 피드를_수정하면_200과_수정된_피드_DTO가_반환된다() throws Exception {

            // given
            UUID userId = UUID.randomUUID();
            User user = UserFixture.createUser();
            ReflectionTestUtils.setField(user, "id", userId);

            Location location = LocationFixture.createLocation();
            Grid grid = location.getGrid();
            Weather weather = WeatherFixture.createWeather(grid);

            UUID feedId = UUID.randomUUID();
            Feed originFeed = FeedFixture.createFeed(user, weather);
            ReflectionTestUtils.setField(originFeed, "id", feedId);

            String newContent = "피드 수정 테스트";

            FeedUpdateRequest request = FeedUpdateRequest.builder()
                .content(newContent)
                .build();

            FeedDto expected = FeedFixture.createFeedDtoWithContent(originFeed, newContent);

            given(feedService.update(any(UUID.class), any(UUID.class), any(FeedUpdateRequest.class)))
                .willReturn(expected);

            // when & then
            mockMvc.perform(patch("/api/feeds/{id}", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("userId", userId.toString())
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feedId.toString()))
                .andExpect(jsonPath("$.content").value(newContent))
                .andExpect(jsonPath("$.author.userId").value(userId.toString()));
        }

        @Test
        void 존재하지_않는_피드라면_404가_반환된다() throws Exception {

            // given
            UUID invalidFeedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String newContent = "피드 수정 테스트";

            FeedUpdateRequest request = FeedUpdateRequest.builder()
                .content(newContent)
                .build();

            given(feedService.update(any(UUID.class), any(UUID.class), any(FeedUpdateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/feeds/{id}", invalidFeedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("userId", userId.toString())
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.FEED_NOT_FOUND.name()));
        }

        @Test
        void 작성자가_아니라면_403이_반환된다() throws Exception {

            // given
            UUID invalidUserId = UUID.randomUUID();
            User user = UserFixture.createUser();

            Location location = LocationFixture.createLocation();
            Grid grid = location.getGrid();
            Weather weather = WeatherFixture.createWeather(grid);

            UUID feedId = UUID.randomUUID();
            Feed originFeed = FeedFixture.createFeed(user, weather);
            ReflectionTestUtils.setField(originFeed, "id", feedId);

            String newContent = "피드 수정 테스트";

            FeedUpdateRequest request = FeedUpdateRequest.builder()
                .content(newContent)
                .build();

            given(feedService.update(any(UUID.class), any(UUID.class), any(FeedUpdateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.FORBIDDEN_FEED_MODIFICATION));

            // when & then
            mockMvc.perform(patch("/api/feeds/{id}", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("userId", invalidUserId.toString())
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.FORBIDDEN_FEED_MODIFICATION.name()));
        }

        @Test
        void 내용이_빈값이면_400이_반환된다() throws Exception {

            // given
            UUID userId = UUID.randomUUID();
            User user = UserFixture.createUser();
            ReflectionTestUtils.setField(user, "id", userId);

            Location location = LocationFixture.createLocation();
            Grid grid = location.getGrid();
            Weather weather = WeatherFixture.createWeather(grid);

            UUID feedId = UUID.randomUUID();
            Feed originFeed = FeedFixture.createFeed(user, weather);
            ReflectionTestUtils.setField(originFeed, "id", feedId);

            String newContent = " ";

            FeedUpdateRequest request = FeedUpdateRequest.builder()
                .content(newContent)
                .build();

            // when & then
            mockMvc.perform(patch("/api/feeds/{id}", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("userId", userId.toString())
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("피드 논리 삭제 테스트")
    class FeedSoftDeleteTest {

        @Test
        void 피드를_논리삭제_하면_204가_반환된다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User user = UserFixture.createUser();
            Location location = LocationFixture.createLocation();
            Grid grid = location.getGrid();
            Weather weather = WeatherFixture.createWeather(grid);
            Feed feed = FeedFixture.createFeed(user, weather);

            /* 컨트롤러 테스트이므로, 반환되는 Feed 객체의 isDeleted는 신경쓰지 않음 */
            given(feedService.delete(eq(feedId), eq(userId))).willReturn(feed);

            // when & then
            mockMvc.perform(delete("/api/feeds/{id}", feedId)
                .param("userId", userId.toString()))
                .andExpect(status().isNoContent());
        }

        @Test
        void 존재하지_않는_피드라면_404가_반환된다() throws Exception {

            // given
            UUID userId = UUID.randomUUID();
            UUID invalidFeedId = UUID.randomUUID();

            given(feedService.delete(eq(invalidFeedId), eq(userId)))
                .willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND));

            // when & then
            mockMvc.perform(delete("/api/feeds/{id}", invalidFeedId)
                .param("userId", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.FEED_NOT_FOUND.name()));
        }

        @Test
        void 이미_삭제_처리된_피드라면_404가_반환된다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            given(feedService.delete(eq(feedId), eq(userId)))
                .willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND));

            // when & then
            mockMvc.perform(delete("/api/feeds/{id}", feedId)
                    .param("userId", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.FEED_NOT_FOUND.name()));
        }

        @Test
        void 작성자가_아니라면_403이_반환된다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            given(feedService.delete(eq(feedId), eq(otherUserId)))
                .willThrow(new OtbooException(ErrorCode.FORBIDDEN_FEED_DELETION));

            // when & then
            mockMvc.perform(delete("/api/feeds/{id}", feedId)
                    .param("userId", otherUserId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.FORBIDDEN_FEED_DELETION.name()));
        }
    }
}
