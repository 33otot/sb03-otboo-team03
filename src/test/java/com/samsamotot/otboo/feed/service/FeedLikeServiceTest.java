package com.samsamotot.otboo.feed.service;


import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.*;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedLike;
import com.samsamotot.otboo.feed.repository.FeedLikeRepository;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.location.entity.Location;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.weather.entity.Grid;
import com.samsamotot.otboo.weather.entity.Weather;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedLike 서비스 단위 테스트")
public class FeedLikeServiceTest {

    @Mock
    private FeedLikeRepository feedLikeRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedLikeServiceImpl feedLikeService;

    User mockUser;
    Feed mockFeed;

    @BeforeEach
    void setUp() {
        mockUser = UserFixture.createUser();
        Location location = LocationFixture.createLocation();
        Grid grid = location.getGrid();
        Weather weather = WeatherFixture.createWeather(grid);
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());
        mockFeed = FeedFixture.createFeed(mockUser, weather);
        ReflectionTestUtils.setField(mockFeed, "id", UUID.randomUUID());
    }

    @Nested
    @DisplayName("피드 좋아요 생성 테스트")
    class FeedLikeCreateTest {

        @Test
        void 피드_좋아요시_좋아요_객체가_반환된다() {

            // given
            UUID userId = mockUser.getId();
            UUID feedId = mockFeed.getId();

            FeedLike feedLike = FeedLikeFixture.createFeedLike(mockUser, mockFeed);

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(feedRepository.findByIdAndIsDeletedFalse(feedId)).willReturn(Optional.of(mockFeed));
            given(feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)).willReturn(false);
            given(feedLikeRepository.save(any(FeedLike.class))).willReturn(feedLike);

            // when
            FeedLike result = feedLikeService.create(feedId, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getFeed()).isEqualTo(mockFeed);
            assertThat(result.getUser()).isEqualTo(mockUser);
        }

        @Test
        void 존재하지_않는_피드에_좋아요시_예외를_던진다() {

            // given
            UUID userId = mockUser.getId();
            UUID invalidFeedId = UUID.randomUUID();

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(feedRepository.findByIdAndIsDeletedFalse(invalidFeedId)).willReturn(Optional.empty());

            // when & then
            Assertions.assertThatThrownBy(() -> feedLikeService.create(invalidFeedId, userId))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_NOT_FOUND);
        }

        @Test
        void 존재하지_않는_사용자가_피드_좋아요시_예외를_던진다() {

            // given
            UUID invalidUserId = mockUser.getId();
            UUID feedId = mockFeed.getId();

            given(userRepository.findById(invalidUserId)).willReturn(Optional.empty());

            // when & then
            Assertions.assertThatThrownBy(() -> feedLikeService.create(feedId, invalidUserId))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        void 이미_좋아요를_누른_피드라면_예외를_던진다() {

            // given
            UUID userId = mockUser.getId();
            UUID feedId = mockFeed.getId();

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(feedRepository.findByIdAndIsDeletedFalse(feedId)).willReturn(Optional.of(mockFeed));
            given(feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)).willReturn(true);

            // when & then
            Assertions.assertThatThrownBy(() -> feedLikeService.create(feedId, userId))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_ALREADY_LIKED);
        }
    }

    @Nested
    @DisplayName("피드 좋아요 취소 테스트")
    class FeedLikeCancelTest {

        @Test
        void 좋아요_취소_요청시_정상적으로_처리되어야_한다() {

            // given
            UUID userId = mockUser.getId();
            UUID feedId = mockFeed.getId();

            given(feedRepository.findByIdAndIsDeletedFalse(feedId)).willReturn(Optional.of(mockFeed));
            given(feedLikeRepository.deleteByFeedIdAndUserId(feedId, userId)).willReturn(1);

            // when
            feedLikeService.delete(feedId, userId);

            // then
            verify(feedRepository, times(1)).findByIdAndIsDeletedFalse(feedId);
            verify(feedLikeRepository, times(1)).deleteByFeedIdAndUserId(feedId, userId);
        }

        @Test
        void 존재하지_않는_피드에_좋아요_취소시_예외를_던진다() {

            // given
            UUID invalidFeedId = UUID.randomUUID();
            UUID userId = mockUser.getId();

            given(feedRepository.findByIdAndIsDeletedFalse(invalidFeedId)).willReturn(Optional.empty());

            // when & then
            Assertions.assertThatThrownBy(() -> feedLikeService.delete(invalidFeedId, userId))
                    .isInstanceOf(OtbooException.class)
                    .extracting(e -> ((OtbooException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FEED_NOT_FOUND);

            verify(feedRepository, times(1)).findByIdAndIsDeletedFalse(invalidFeedId);
            verifyNoInteractions(feedLikeRepository);
        }

        @Test
        void 좋아요_정보가_없는_상태에서_좋아요_취소시_예외를_던진다() {

            // given
            UUID feedId = mockFeed.getId();
            UUID userId = mockUser.getId();

            given(feedRepository.findByIdAndIsDeletedFalse(feedId)).willReturn(Optional.of(mockFeed));
            given(feedLikeRepository.deleteByFeedIdAndUserId(feedId, userId)).willReturn(0);

            // when & then
            Assertions.assertThatThrownBy(() -> feedLikeService.delete(feedId, userId))
                .isInstanceOf(OtbooException.class)
                .extracting(e -> ((OtbooException) e).getErrorCode())
                .isEqualTo(ErrorCode.FEED_LIKE_NOT_FOUND);

            verify(feedRepository, times(1)).findByIdAndIsDeletedFalse(feedId);
            verify(feedLikeRepository, times(1)).deleteByFeedIdAndUserId(feedId, userId);
        }
    }
}