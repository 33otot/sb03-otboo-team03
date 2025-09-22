package com.samsamotot.otboo.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentCursorRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.service.CommentService;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.CommentFixture;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.user.dto.AuthorDto;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
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

            given(commentService.create(any(UUID.class),any(CommentCreateRequest.class))).willReturn(commentDto);

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/comments", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.userId").value(authorId.toString()))
                .andExpect(jsonPath("$.feedId").value(feedId.toString()))
                .andExpect(jsonPath("$.content").value(content));

            verify(commentService).create(eq(feedId),any(CommentCreateRequest.class));
        }

        @Test
        void 존재하지_않는_유저면_404가_반환되어야_한다() throws Exception {

            // given
            UUID invalidUserId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(invalidUserId)
                .content(content)
                .build();

            given(commentService.create(any(UUID.class), any(CommentCreateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/comments", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.USER_NOT_FOUND.toString()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));

            verify(commentService).create(eq(feedId),any(CommentCreateRequest.class));
        }

        @Test
        void 존재하지_않는_피드면_404가_반환되어야_한다() throws Exception {

            // given
            UUID authorId = UUID.randomUUID();
            UUID invalidFeedId = UUID.randomUUID();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(authorId)
                .content(content)
                .build();

            given(commentService.create(any(UUID.class), any(CommentCreateRequest.class)))
                .willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/comments", invalidFeedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.FEED_NOT_FOUND.toString()))
                .andExpect(jsonPath("$.message").value(ErrorCode.FEED_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.FEED_NOT_FOUND.getCode()));

            verify(commentService).create(eq(invalidFeedId),any(CommentCreateRequest.class));
        }

        @Test
        void 본문_길이가_1000자를_초과하면_400이_반환되어야_한다() throws Exception {

            // given
            UUID authorId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();
            String over = "a".repeat(1001); // 1001자

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(authorId)
                .content(over)
                .build();

            // when & then
            mockMvc.perform(post("/api/feeds/{feedId}/comments", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(commentService);
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회 테스트")
    class CommentListGetTest {

        @Test
        void 댓글_목록_조회_성공_시_정렬된_데이터와_200이_반환된다() throws Exception {

            // given
            Instant base = Instant.parse("2025-09-16T10:00:00Z");
            UUID feedId = UUID.randomUUID();
            CommentDto oldComment = CommentFixture.createCommentDtoWithCreatedAt("content1", null, feedId, base.minusSeconds(1000));
            CommentDto newComment = CommentFixture.createCommentDtoWithCreatedAt("content2", null, feedId, base);
            List<CommentDto> commentDtos = List.of(newComment, oldComment);

            CursorResponse<CommentDto> expected = new CursorResponse<>(
                commentDtos,
                null,
                null,
                false,
                2L,
                "createdAt",
                SortDirection.DESCENDING
            );

            given(commentService.getComments(any(UUID.class), any(CommentCursorRequest.class)))
                .willReturn(expected);

            // when & then
            mockMvc.perform(get("/api/feeds/{feedId}/comments", feedId)
                    .param("limit", String.valueOf(10))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].content").value("content2"))
                .andExpect(jsonPath("$.data[1].content").value("content1"))
                .andExpect(jsonPath("$.totalCount").value(2L));
        }

        @Test
        void 커서_조건이_적용된_댓글목록과_200이_반환된다() throws Exception {

            // given
            UUID feedId = UUID.randomUUID();
            Instant base = Instant.parse("2025-09-16T10:00:00Z");

            CommentDto cursorCommentDto = CommentFixture.createCommentDtoWithCreatedAt("cursor", null, feedId, base);
            CommentDto commentDto1 = CommentFixture.createCommentDtoWithCreatedAt("content1", null, feedId, base.plusSeconds(1000));
            CommentDto commentDto2 = CommentFixture.createCommentDtoWithCreatedAt("content2", null, feedId, base.plusSeconds(2000));
            List<CommentDto> commentDtos = List.of(commentDto2, commentDto1);

            String cursor = cursorCommentDto.createdAt().toString();
            UUID idAfter = UUID.randomUUID();

            CursorResponse<CommentDto> expected = new CursorResponse<>(
                commentDtos,
                null,
                null,
                false,
                2L,
                "createdAt",
                SortDirection.DESCENDING
            );

            given(commentService.getComments(any(UUID.class), any(CommentCursorRequest.class))).willReturn(expected);

            ArgumentCaptor<CommentCursorRequest> captor = ArgumentCaptor.forClass(CommentCursorRequest.class);

            // when & then
            mockMvc.perform(get("/api/feeds/{feedId}/comments", feedId)
                    .param("cursor", cursor)
                    .param("idAfter", String.valueOf(idAfter))
                    .param("limit", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(commentDtos.size())))
                .andExpect(jsonPath("$.data[0].content").value("content2"))
                .andExpect(jsonPath("$.totalCount").value(2L));

            verify(commentService).getComments(any(UUID.class), captor.capture());
            CommentCursorRequest actual = captor.getValue();
            assertThat(actual.cursor()).isEqualTo(cursor);
            assertThat(actual.idAfter()).isEqualTo(idAfter);
            assertThat(actual.limit()).isEqualTo(10);
        }

        @Test
        void 존재하지_않는_피드ID로_조회하면_404가_반환된다() throws Exception {

            // given
            UUID invalidFeedId = UUID.randomUUID();

            given(commentService.getComments(any(UUID.class), any(CommentCursorRequest.class)))
                .willThrow(new OtbooException(ErrorCode.FEED_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/feeds/{feedId}/comments", invalidFeedId)
                   .param("limit", String.valueOf(10))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionName").value(ErrorCode.FEED_NOT_FOUND.toString()));
        }

        @Test
        void limit_조건이_없으면_400이_반환된다() throws Exception {

            mockMvc.perform(get("/api/feeds/{feedId}/comments", UUID.randomUUID())
                )
                .andExpect(status().isBadRequest());

            verifyNoInteractions(commentService);
        }
    }
}
