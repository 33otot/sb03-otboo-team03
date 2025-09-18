package com.samsamotot.otboo.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.comment.entity.Comment;
import com.samsamotot.otboo.comment.mapper.CommentMapper;
import com.samsamotot.otboo.comment.repository.CommentRepository;
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
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Weather;
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
    private CommentService commentService;

    User mockUser;
    Feed mockFeed;


    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createUser();
        Location mockLocation = LocationFixture.createLocation();
        Weather mockWeather = WeatherFixture.createWeather(mockLocation);
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
                .feedId(feedId)
                .content(content)
                .build();
            UUID commentId = UUID.randomUUID();
            Comment savedComment = CommentFixture.createCommentWithContent(mockFeed, mockUser,
                content);
            ReflectionTestUtils.setField(savedComment, "id", commentId);
            CommentDto expectedDto = CommentFixture.createCommentDto(savedComment);

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(mockUser));
            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.of(mockFeed));
            given(commentRepository.save(any(Comment.class))).willReturn(savedComment);
            given(commentMapper.toDto(any(Comment.class))).willReturn(expectedDto);

            // when
            CommentDto result = commentService.create(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(commentId);
            assertThat(result.author().userId()).isEqualTo(authorId);
            assertThat(result.feedId()).isEqualTo(feedId);
            assertThat(result.content()).isEqualTo(content);
        }

        @Test
        void 댓글을_등록할_때_존재하지_않는_유저면_예외가_발생한다() {

            // given
            UUID invalidUserId = UUID.randomUUID();
            UUID feedId = mockFeed.getId();
            String content = "댓글 생성 테스트";

            CommentCreateRequest request = CommentCreateRequest.builder()
                .authorId(invalidUserId)
                .feedId(feedId)
                .content(content)
                .build();

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.create(request))
                .isInstanceOf(OtbooException.class)
                .extracting(e ->  ((OtbooException) e).getErrorCode())
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
                .feedId(invalidFeedId)
                .content(content)
                .build();

            given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(mockUser));
            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.create(request))
                .isInstanceOf(OtbooException.class)
                .extracting(e ->  ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_NOT_FOUND);
        }
    }
}
