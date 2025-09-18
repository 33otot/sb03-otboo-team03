package com.samsamotot.otboo.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.service.CommentService;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.feed.controller.FeedController;
import com.samsamotot.otboo.user.dto.AuthorDto;
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
@DisplayName("Comment 컨트롤러 슬라이스 테스트")
public class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @Nested
    @DisplayName("댓글 생성 테스트")
    class CommentCreateTest {

        @Test
        void 유효한_등록_요청이면_등록_성공후_201과_DTO가_반환되어야_한다() throws Exception {

            // given
            UUID authorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(authorId)
                .feedId(feedId)
                .content(content)
                .build();

            AuthorDto authorDto = AuthorDto.builder()
                .userId(authorId)
                .build();

            CommentDto commentDto = CommentDto.builder()
                .author(authorDto)
                .feedId(feedId)
                .content(content)
                .build();

            given(commentService.create(any(CommentCreateRequest.class))).willReturn(commentDto);

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/comments}", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.userId").value(authorId.toString()))
                .andExpect(jsonPath("$.feedId").value(feedId.toString()))
                .andExpect(jsonPath("$.content").value(content));
        }

        @Test
        void 존재하지_않는_유저면_404가_반환되어야_한다() throws Exception {

            // given
            UUID invalidUserId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(invalidUserId)
                .feedId(feedId)
                .content(content)
                .build();

            given(commentService.create(any(CommentCreateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/comments}", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.USER_NOT_FOUND.toString()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        }

        @Test
        void 존재하지_않는_피드면_404가_반환되어야_한다() throws Exception {

            // given
            UUID authorId = UUID.randomUUID();
            UUID invalidFeedId = UUID.randomUUID();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(authorId)
                .feedId(invalidFeedId)
                .content(content)
                .build();

            given(commentService.create(any(CommentCreateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/comments}", invalidFeedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.FEED_NOT_FOUND.toString()))
                .andExpect(jsonPath("$.message").value(ErrorCode.FEED_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.FEED_NOT_FOUND.getCode()));
        }

        @Test
        void 본문_길이가_1000자를_초과하면_400이_반환되어야_한다() throws Exception {

            // given
            UUID authorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String over = "a".repeat(1001); // 1001자

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(authorId)
                .feedId(feedId)
                .content(over)
                .build();

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/comments}", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(commentService);
        }
    }
}
