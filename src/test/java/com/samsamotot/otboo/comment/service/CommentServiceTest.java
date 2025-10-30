package com.samsamotot.otboo.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentCursorRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.comment.mapper.CommentMapper;
import com.samsamotot.otboo.comment.repository.CommentRepository;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.CommentFixture;
import com.samsamotot.otboo.common.fixture.FeedFixture;
import com.samsamotot.otboo.common.fixture.LocationFixture;
import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.common.fixture.WeatherFixture;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.notification.entity.Notification;
import com.samsamotot.otboo.notification.repository.NotificationRepository;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
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
@DisplayName("Comment 서비스 단위 테스트")
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    User mockUser;
    Feed mockFeed;


    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createUser();
        Location mockLocation = LocationFixture.createLocation();
        Grid grid = mockLocation.getGrid();
        Weather mockWeather = WeatherFixture.createWeather(grid);
        mockFeed = FeedFixture.createFeed(mockUser, mockWeather);

        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(mockFeed, "id", UUID.randomUUID());
    }

    @Nested
    @DisplayName("댓글 생성 테스트")
    class CommentCreateTest {

        @Test
        void 댓글을_생성하면_CommentDto를_반환해야_한다() {

            // given
            UUID authorId = mockUser.getId();
            UUID feedId = mockFeed.getId();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(authorId)
                .content(content)
                .build();
            UUID commentId = UUID.randomUUID();
            Comment savedComment = CommentFixture.createCommentWithContent(mockFeed, mockUser,
                content);
            ReflectionTestUtils.setField(savedComment, "id", commentId);
            CommentDto expectedDto = CommentFixture.createCommentDto(savedComment);

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(mockUser));
            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.of(mockFeed));
            given(commentRepository.save(any(Comment.class))).willReturn(savedComment);
            given(commentMapper.toDto(any(Comment.class))).willReturn(expectedDto);

            // when
            CommentDto result = commentService.create(feedId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(commentId);
            assertThat(result.author().userId()).isEqualTo(authorId);
            assertThat(result.feedId()).isEqualTo(feedId);
            assertThat(result.content()).isEqualTo(content);
            verify(feedRepository, times(1)).incrementCommentCount(feedId);
        }

        @Test
        void 댓글을_등록할_때_존재하지_않는_유저면_예외가_발생한다() {

            // given
            UUID invalidUserId = UUID.randomUUID();
            UUID feedId = mockFeed.getId();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(invalidUserId)
                .content(content)
                .build();

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.create(feedId, request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        void 댓글을_등록할_때_존재하지_않는_피드면_예외가_발생한다() {

            // given
            UUID userId = mockUser.getId();
            UUID invalidFeedId = UUID.randomUUID();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(userId)
                .content(content)
                .build();

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(mockUser));
            given(feedRepository.findByIdAndIsDeletedFalse(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.create(invalidFeedId, request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회 테스트")
    class CommentListGetTest {

        @Test
        void 커서없이_조회시_레포지토리를_호출한다() {

            // given
            UUID feedId = mockFeed.getId();
            CommentCursorRequest request = createDefaultRequest();

            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(mockFeed));
            given(commentRepository.findByFeedIdWithCursor(any(UUID.class), any(), any(), anyInt())).willReturn(List.of());
            given(commentRepository.countByFeedId(any(UUID.class))).willReturn(1L);

            // when
            commentService.getComments(feedId, request);

            // then
            verify(feedRepository).findById(any(UUID.class));
            verify(commentRepository).findByFeedIdWithCursor(
                eq(feedId),
                isNull(String.class),
                isNull(UUID.class),
                eq(request.limit() + 1)
            );
        }

        @Test
        void 커서가_있을_때_올바른_값으로_레포지토리를_호출한다() {

            // given
            String cursor = "2025-09-18T10:00:00Z";
            UUID idAfter = UUID.randomUUID();
            int limit = 20;

            CommentCursorRequest request = CommentCursorRequest.builder()
                .cursor(cursor)
                .idAfter(idAfter)
                .limit(limit)
                .build();

            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(mockFeed));
            given(commentRepository.findByFeedIdWithCursor(any(UUID.class), any(), any(), anyInt())).willReturn(List.of());
            given(commentRepository.countByFeedId(any(UUID.class))).willReturn(1L);

            // when
            commentService.getComments(mockFeed.getId(), request);

            // then
            // 서비스가 요청 DTO의 커서 값들을 레포지토리 메서드에 그대로 전달했는지 검증
            verify(commentRepository).findByFeedIdWithCursor(
                eq(mockFeed.getId()),
                eq(cursor),
                eq(idAfter),
                eq(limit + 1)
            );
        }

        @Test
        void 존재하지_않는_피드라면_예외가_발생한다() {

            // given
            UUID invalidFeedId = UUID.randomUUID();
            CommentCursorRequest request = createDefaultRequest();

            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.getComments(invalidFeedId, request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_NOT_FOUND);
        }

        @Test
        void hasNext_true면_nextCursor와_nextIdAfter를_생성한다() {

            // given
            int limit = 10;
            CommentCursorRequest request = CommentCursorRequest.builder()
                .limit(limit)
                .cursor(null)
                .idAfter(null)
                .build();

            List<Comment> comments = new ArrayList<>();
            for (int i = 0; i < limit + 1; i++) {
                Comment comment = CommentFixture.createComment(mockFeed, mockUser);
                ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(comment, "createdAt", Instant.now().minusSeconds(i));
                comments.add(comment);
            }

            List<CommentDto> expectedDtos = comments.subList(0, limit).stream()
                .map(CommentFixture::createCommentDto)
                .toList();

            Comment lastComment = comments.get(limit - 1);

            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(mockFeed));
            given(commentRepository.findByFeedIdWithCursor(any(UUID.class), any(), any(), eq(limit + 1))).willReturn(comments);
            given(commentRepository.countByFeedId(any(UUID.class))).willReturn(11L);

            for (int i = 0; i < limit; i++) {
                given(commentMapper.toDto(comments.get(i))).willReturn(expectedDtos.get(i));
            }

            // when
            CursorResponse<CommentDto> response = commentService.getComments(mockFeed.getId(), request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.hasNext()).isTrue();
            assertThat(response.data()).hasSize(limit);
            assertThat(response.nextCursor()).isEqualTo(lastComment.getCreatedAt().toString());
            assertThat(response.nextIdAfter()).isEqualTo(lastComment.getId());
        }

        @Test
        void hasNext_false면_nextCursor와_nextIdAfter는_null이다() {

            // given
            int limit = 10;
            CommentCursorRequest request = CommentCursorRequest.builder()
                .limit(limit)
                .cursor(null)
                .idAfter(null)
                .build();

            List<Comment> comments = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                Comment comment = CommentFixture.createComment(mockFeed, mockUser);
                ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(comment, "createdAt", Instant.now().minusSeconds(i));
                comments.add(comment);
            }

            List<CommentDto> expectedDtos = comments.subList(0, limit).stream()
                .map(CommentFixture::createCommentDto)
                .toList();

            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(mockFeed));
            given(commentRepository.findByFeedIdWithCursor(any(UUID.class), any(), any(), eq(limit + 1))).willReturn(comments);
            given(commentRepository.countByFeedId(any(UUID.class))).willReturn(10L);

            for (int i = 0; i < limit; i++) {
                given(commentMapper.toDto(comments.get(i))).willReturn(expectedDtos.get(i));
            }

            // when
            CursorResponse<CommentDto> response = commentService.getComments(mockFeed.getId(), request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.data()).hasSize(limit);
            assertThat(response.nextCursor()).isNull();
            assertThat(response.nextIdAfter()).isNull();
        }

        @Test
        void 커서_포맷이_잘못되면_예외가_발생한다() {

            // given
            int limit = 10;
            String invalidCursor = "invalidCursor";

            CommentCursorRequest request = CommentCursorRequest.builder()
                .cursor(invalidCursor)
                .idAfter(null)
                .limit(limit)
                .build();

            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(mockFeed));

            // when
            assertThatThrownBy(() -> commentService.getComments(mockFeed.getId(), request))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT);
        }

        @Test
        void 조회할_댓글이_전혀_없을_때_빈_리스트와_null_커서를_반환한다() {

            // given
            CommentCursorRequest request = createDefaultRequest();

            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(mockFeed));
            given(commentRepository.findByFeedIdWithCursor(any(UUID.class), any(), any(), anyInt())).willReturn(List.of());

            // when
            CursorResponse<CommentDto> response = commentService.getComments(mockFeed.getId(), request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.data()).isEmpty();
            assertThat(response.nextCursor()).isNull();
            assertThat(response.nextIdAfter()).isNull();
        }

        private CommentCursorRequest createDefaultRequest() {
            return CommentCursorRequest.builder()
                .limit(20)
                .cursor(null)
                .idAfter(null)
                .build();
        }
    }
}