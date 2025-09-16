package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowListResponse;
import com.samsamotot.otboo.follow.dto.FollowingRequest;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.follow.mapper.FollowMapper;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * PackageName  : com.samsamotot.otboo.follow.service
 * FileName     : FollowServiceImplTest
 * Author       : dounguk
 * Date         : 2025. 9. 13.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("팔로우 서비스 단위 테스트")
class FollowServiceImplTest {
    @InjectMocks
    private FollowServiceImpl followService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowMapper followMapper;


    /*
        팔로우 생성 단위 테스트
     */
    @Test
    void 팔로우_생성한다() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowCreateRequest req = new FollowCreateRequest(followerId, followeeId);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
            .willReturn(false);

        User follower = mock(User.class, Answers.RETURNS_DEFAULTS);
        User followee = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(follower.isLocked()).thenReturn(false);
        lenient().when(followee.isLocked()).thenReturn(false);

        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));

        given(followRepository.save(any(Follow.class))).willAnswer(inv -> inv.getArgument(0));

        given(followMapper.toDto(any(Follow.class))).willAnswer(inv -> {
            return new FollowDto(
                UUID.randomUUID(),
                new AuthorDto(followeeId, "followeeName", null),
                new AuthorDto(followerId, "followerName", null)
            );
        });

        // when
        FollowDto dto = followService.follow(req);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.followee()).isNotNull();
        assertThat(dto.follower()).isNotNull();
        assertThat(dto.followee().userId()).isEqualTo(followeeId);
        assertThat(dto.follower().userId()).isEqualTo(followerId);
    }

    @Test
    void 중복_팔로우_확인한다_실패() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowCreateRequest req = new FollowCreateRequest(followerId, followeeId);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
            .willReturn(true);

        // when & then
        assertThatThrownBy(() -> followService.follow(req))
            .isInstanceOf(OtbooException.class);
    }

    @Test
    void follower_있는지_확인한다_실패() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowCreateRequest req = new FollowCreateRequest(followerId, followeeId);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
            .willReturn(false);

        given(userRepository.findById(followerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.follow(req))
            .isInstanceOf(OtbooException.class);
    }

    @Test
    void followee_있는지_확인한다_실패() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowCreateRequest req = new FollowCreateRequest(followerId, followeeId);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
            .willReturn(false);

        User follower = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(follower.isLocked()).thenReturn(false);
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));

        given(userRepository.findById(followeeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.follow(req))
            .isInstanceOf(OtbooException.class);
    }

    @Test
    void follower_논리삭제_확인한다_실패() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowCreateRequest req = new FollowCreateRequest(followerId, followeeId);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
            .willReturn(false);

        User follower = mock(User.class, Answers.RETURNS_DEFAULTS);
        User followee = mock(User.class, Answers.RETURNS_DEFAULTS);

        lenient().when(follower.isLocked()).thenReturn(true);
        lenient().when(followee.isLocked()).thenReturn(false);

        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));

        // when & then
        assertThatThrownBy(() -> followService.follow(req))
            .isInstanceOf(OtbooException.class);
    }

    @Test
    void followee_논리삭제_확인한다_실패() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowCreateRequest req = new FollowCreateRequest(followerId, followeeId);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
            .willReturn(false);

        User follower = mock(User.class, Answers.RETURNS_DEFAULTS);
        User followee = mock(User.class, Answers.RETURNS_DEFAULTS);

        lenient().when(follower.isLocked()).thenReturn(false);
        lenient().when(followee.isLocked()).thenReturn(true);

        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));

        // when & then
        assertThatThrownBy(() -> followService.follow(req))
            .isInstanceOf(OtbooException.class);
    }


    /*
        팔로잉 목록 조회 단위 테스트
     */

    @Test
    void 팔로잉_목록_조회를_한다() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        FollowingRequest req = new FollowingRequest(followerId, null, null, 10, null);

        User follower = mock(User.class, Answers.RETURNS_DEFAULTS);
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        lenient().when(follower.isLocked()).thenReturn(false);

        Follow f1 = mock(Follow.class, Answers.RETURNS_DEFAULTS);
        Follow f2 = mock(Follow.class, Answers.RETURNS_DEFAULTS);
        given(followRepository.findFollowings(any(FollowingRequest.class)))
            .willReturn(List.of(f1, f2));

        given(followRepository.countTotalElements(followerId, null))
            .willReturn(2L);

        given(followMapper.toDto(any(Follow.class))).willAnswer(inv ->
            new FollowDto(
                UUID.randomUUID(),
                new AuthorDto(UUID.randomUUID(), "followee", null),
                new AuthorDto(followerId, "follower", null)
            )
        );

        // when
        FollowListResponse res = followService.getFollowings(req);

        // then
        assertThat(res).isNotNull();
        assertThat(res.data()).isNotNull();
    }

    @Test
    void 유저가_있는지_확인한다() throws Exception {
        /// given
        UUID followerId = UUID.randomUUID();
        FollowingRequest req = new FollowingRequest(followerId, null, null, 10, null);

        given(userRepository.findById(followerId)).willReturn(Optional.empty());

        // when n then
        assertThatThrownBy(() -> followService.getFollowings(req))
            .isInstanceOf(OtbooException.class);
    }

    @Test
    void 유저의_locked_여부를_확인한다() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        FollowingRequest req = new FollowingRequest(followerId, null, null, 10, null);

        User follower = mock(User.class, Answers.RETURNS_DEFAULTS);
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(follower.isLocked()).willReturn(true); // 잠금

        // when n then
        assertThatThrownBy(() -> followService.getFollowings(req))
            .isInstanceOf(OtbooException.class);
    }
}