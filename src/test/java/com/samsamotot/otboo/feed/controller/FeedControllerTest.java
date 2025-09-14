package com.samsamotot.otboo.feed.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.clothes.dto.OotdDto;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.fixture.FeedFixture;
import com.samsamotot.otboo.feed.service.FeedService;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.weather.dto.WeatherDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
}
