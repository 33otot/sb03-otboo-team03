package com.samsamotot.otboo.feed.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samsamotot.otboo.common.config.SecurityTestConfig;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.feed.entity.FeedLike;
import com.samsamotot.otboo.feed.service.FeedLikeService;
import com.samsamotot.otboo.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedLikeController.class)
@Import(SecurityTestConfig.class)
@DisplayName("FeedLike 컨트롤러 슬라이스 테스트")
public class FeedLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedLikeService feedLikeService;

    User mockUser;
    CustomUserDetails mockPrincipal;

    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createValidUser();
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        mockPrincipal = new CustomUserDetails(mockUser);
    }

    @Nested
    @DisplayName("피드 좋아요 생성 테스트")
    class FeedLikeCreateTest {

        @Test
        void 피드에_좋아요시_204가_반환되어야_한다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = mockUser.getId();

            FeedLike mockFeedLike = mock(FeedLike.class);
            given(mockFeedLike.getId()).willReturn(UUID.randomUUID());
            given(feedLikeService.create(eq(feedId), eq(userId))).willReturn(mockFeedLike);

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
                    .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
        }

        @Test
        void 존재하지_않는_피드에_좋아요시_404가_반환되어야_한다() throws Exception {

            // given
            UUID invalidFeedId = UUID.randomUUID();
            UUID userId = mockUser.getId();

            given(feedLikeService.create(eq(invalidFeedId), eq(userId)))
                .willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/like", invalidFeedId)
                    .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
        }

        @Test
        void 존재하지_않는_유저가_좋아요시_404가_반환되어야_한다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID invalidUserId = mockUser.getId();

            given(feedLikeService.create(eq(feedId), eq(invalidUserId)))
                .willThrow(new OtbooException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
                    .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
        }

        @Test
        void 이미_좋아요가_존재한다면_409가_반환되어야_한다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = mockUser.getId();

            given(feedLikeService.create(eq(feedId), eq(userId)))
                .willThrow(new OtbooException(ErrorCode.FEED_ALREADY_LIKED));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
                    .with(user(mockPrincipal)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("피드 좋아요 취소 테스트")
    class FeedLikeCancelTest {

        @Test
        void 피드에_좋아요_취소시_204가_반환되어야_한다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = mockUser.getId();

            willDoNothing()
                .given(feedLikeService)
                .delete(eq(feedId), eq(userId));

            // when & then
            mockMvc.perform(delete("/api/feeds/{feedId}/like", feedId)
                    .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
        }

        @Test
        void 존재하지_않는_피드에_좋아요_취소_요청시_404가_반환되어야_한다() throws Exception {

            // given
            UUID invalidFeedId = UUID.randomUUID();
            UUID userId = mockUser.getId();

            willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND))
                .given(feedLikeService)
                .delete(eq(invalidFeedId), eq(userId));

            // when & then
            mockMvc.perform(delete("/api/feeds/{feedId}/like", invalidFeedId)
                    .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
        }

        @Test
        void 좋아요를_누르지_않은_피드에_좋아요_취소_요청시_404가_반환되어야_한다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            UUID userId = mockUser.getId();

            willThrow(new OtbooException(ErrorCode.FEED_LIKE_NOT_FOUND))
                .given(feedLikeService)
                .delete(eq(feedId), eq(userId));

            // when & then
            mockMvc.perform(delete("/api/feeds/{feedId}/like", feedId)
                    .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
        }
    }
}
