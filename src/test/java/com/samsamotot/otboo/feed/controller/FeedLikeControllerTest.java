package com.samsamotot.otboo.feed.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.feed.service.FeedLikeService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedLikeController.class)
@DisplayName("FeedLike 컨트롤러 슬라이스 테스트")
public class FeedLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedLikeService feedLikeService;

    @Nested
    @DisplayName("피드 좋아요 생성 테스트")
    class FeedLikeCreateTest {

        @Test
        void 피드에_좋아요시_204가_반환되어야_한다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
                    .param("userId", userId.toString()))
                .andExpect(status().isNoContent());
        }

        @Test
        void 존재하지_않는_피드에_좋아요시_404가_반환되어야_한다() throws Exception {

            // given
            UUID invalidFeedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            given(feedLikeService.create(eq(invalidFeedId), eq(userId)))
                .willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/like", invalidFeedId)
                    .param("userId", userId.toString()))
                .andExpect(status().isNotFound());
        }

        @Test
        void 존재하지_않는_유저가_좋아요시_404가_반환되어야_한다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID invalidUserId = UUID.randomUUID();

            given(feedLikeService.create(eq(feedId), eq(invalidUserId)))
                .willThrow(new OtbooException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
                    .param("userId", invalidUserId.toString()))
                .andExpect(status().isNotFound());
        }

        @Test
        void 이미_좋아요가_존재한다면_409가_반환되어야_한다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            given(feedLikeService.create(eq(feedId), eq(userId)))
                .willThrow(new OtbooException(ErrorCode.FEED_ALREADY_LIKED));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
                    .param("userId", userId.toString()))
                .andExpect(status().isConflict());
        }
    }
}
