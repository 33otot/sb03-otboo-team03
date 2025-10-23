package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.follow.dto.*;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.follow.mapper.FollowMapper;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.user.dto.AuthorDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * PackageName  : com.samsamotot.otboo.follow.service
 * FileName     : FollowServiceImplTest
 * Author       : dounguk
 * Date         : 2025. 9. 13.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Follow 서비스 단위 테스트")
class FollowServiceImplTest {
    @InjectMocks
    private FollowServiceImpl followService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowMapper followMapper;

    private UUID loggedInUserId;
    private final String myEmail = "me@example.com";
    private final UUID myId = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private static void loginAsEmail(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static Instant invokeParse(String cursor) {
        try {
            Method m = FollowServiceImpl.class.getDeclaredMethod("parseCursorToInstant", String.class);
            m.setAccessible(true);
            return (Instant) m.invoke(null, cursor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUpAuth() {
        // 테스트마다 고정된(또는 랜덤) UUID를 인증 컨텍스트에 심어줌
        loggedInUserId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
            loggedInUserId.toString(), "N/A", List.of()  // getName() == UUID 문자열
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

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
        팔로우 요약 정보 조회 단위 테스트
     */
    @Test
    void 로그인_유저정보_확인_실패() throws Exception {
        // given
        loginAsEmail(myEmail);

        User principal = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(principal.getId()).thenReturn(myId);
        lenient().when(principal.isLocked()).thenReturn(false);
        given(userRepository.findByEmail(myEmail)).willReturn(Optional.of(principal));

        given(userRepository.findById(myId)).willReturn(Optional.empty());

        // when n then
        assertThatThrownBy(() -> followService.findFollowSummaries(UUID.randomUUID()))
            .isInstanceOf(OtbooException.class);
    }

    @Test
    void 입력_유저정보_확인_실패() throws Exception {
        // given
        loginAsEmail(myEmail);
        User principal = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(principal.getId()).thenReturn(myId);
        lenient().when(principal.isLocked()).thenReturn(false);
        given(userRepository.findByEmail(myEmail)).willReturn(Optional.of(principal));

        User loggedIn = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(loggedIn.isLocked()).thenReturn(false);

        given(userRepository.findById(any(UUID.class)))
            .willReturn(Optional.of(loggedIn))
            .willReturn(Optional.empty());

        // when n then
        assertThatThrownBy(() -> followService.findFollowSummaries(UUID.randomUUID()))
            .isInstanceOf(OtbooException.class);
    }


    @Test
    void 로그인_유저_locked_확인_실패() throws Exception {
        // given
        loginAsEmail(myEmail);
        User principal = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(principal.getId()).thenReturn(myId);
        lenient().when(principal.isLocked()).thenReturn(false);
        given(userRepository.findByEmail(myEmail)).willReturn(Optional.of(principal));

        User loggedIn = mock(User.class, Answers.RETURNS_DEFAULTS);
        given(loggedIn.isLocked()).willReturn(true);
        given(userRepository.findById(myId)).willReturn(Optional.of(loggedIn));

        // when n then
        assertThatThrownBy(() -> followService.findFollowSummaries(UUID.randomUUID()))
            .isInstanceOf(OtbooException.class);
    }


    @Test
    void 입력_유저_locked_확인_실패() throws Exception {
        // given
        loginAsEmail(myEmail);
        User principal = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(principal.getId()).thenReturn(myId);
        lenient().when(principal.isLocked()).thenReturn(false);
        given(userRepository.findByEmail(myEmail)).willReturn(Optional.of(principal));

        User loggedIn = mock(User.class, Answers.RETURNS_DEFAULTS);
        User target   = mock(User.class, Answers.RETURNS_DEFAULTS);
        given(loggedIn.isLocked()).willReturn(false);
        given(target.isLocked()).willReturn(true);

        given(userRepository.findById(any(UUID.class)))
            .willReturn(Optional.of(loggedIn))
            .willReturn(Optional.of(target));

        // when n then
        assertThatThrownBy(() -> followService.findFollowSummaries(UUID.randomUUID()))
            .isInstanceOf(OtbooException.class);
    }


    @Test
    void 팔로우_있는지_확인_실패() throws Exception {
        // given
        loginAsEmail(myEmail);
        User principal = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(principal.getId()).thenReturn(myId);
        lenient().when(principal.isLocked()).thenReturn(false);
        given(userRepository.findByEmail(myEmail)).willReturn(Optional.of(principal));

        User loggedIn = mock(User.class, Answers.RETURNS_DEFAULTS);
        User target = mock(User.class, Answers.RETURNS_DEFAULTS);
        UUID targetId = UUID.randomUUID();

        given(loggedIn.isLocked()).willReturn(false);
        given(target.isLocked()).willReturn(false);

        given(userRepository.findById(any(UUID.class)))
            .willReturn(Optional.of(loggedIn)) // me
            .willReturn(Optional.of(target));  // target

        given(followRepository.existsByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class)))
            .willReturn(false);

        given(followRepository.findByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class)))
            .willReturn(Optional.empty());

        given(followRepository.countByFolloweeId(any(UUID.class))).willReturn(0L);
        given(followRepository.countByFollowerId(any(UUID.class))).willReturn(0L);

        // when
        FollowSummaryDto response = followService.findFollowSummaries(targetId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.followedByMe()).isFalse();
        assertThat(response.followedByMeId()).isNull();
        assertThat(response.followingMe()).isFalse();
    }


    @Test
    void 팔로우_여부_확인_실패() throws Exception {
        // given
        loginAsEmail(myEmail);
        User principal = mock(User.class, Answers.RETURNS_DEFAULTS);
        lenient().when(principal.getId()).thenReturn(myId);
        lenient().when(principal.isLocked()).thenReturn(false);
        given(userRepository.findByEmail(myEmail)).willReturn(Optional.of(principal));

        User loggedIn = mock(User.class, Answers.RETURNS_DEFAULTS);
        User target   = mock(User.class, Answers.RETURNS_DEFAULTS);
        UUID targetId = UUID.randomUUID();

        given(loggedIn.isLocked()).willReturn(false);
        given(target.isLocked()).willReturn(false);

        given(userRepository.findById(any(UUID.class)))
            .willReturn(Optional.of(loggedIn))
            .willReturn(Optional.of(target));

        given(followRepository.existsByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class)))
            .willReturn(false);

        Follow follow = mock(Follow.class, Answers.RETURNS_DEFAULTS);
        given(follow.getId()).willReturn(UUID.randomUUID());
        given(followRepository.findByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class)))
            .willReturn(Optional.of(follow));

        given(followRepository.countByFolloweeId(any(UUID.class))).willReturn(0L);
        given(followRepository.countByFollowerId(any(UUID.class))).willReturn(0L);

        // when
        FollowSummaryDto res = followService.findFollowSummaries(targetId);

        // then
        assertThat(res).isNotNull();
        assertThat(res.followedByMe()).isTrue();
        assertThat(res.followedByMeId()).isNotNull();
        assertThat(res.followingMe()).isFalse();
    }


    @Test
    void 인증_없으면_실패() throws Exception{
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> followService.findFollowSummaries(UUID.randomUUID()))
            .isInstanceOf(OtbooException.class)
            .hasMessageContaining("인증이 필요");
    }

    /*
        팔로잉 목록 조회 단위 테스트
     */

    @Test
    void 팔로잉_목록_조회_한다() throws Exception {
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

        given(followRepository.countTotalFollowings(followerId, null))
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

    @Test
    void OffsetDateTime_으로_cursor_입력_받을수_있다() throws Exception {
        // given
        String cursor = "2025-09-16T12:34:56+09:00";

        // when
        Instant actual = invokeParse(cursor);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(Instant.parse("2025-09-16T03:34:56Z"));
    }

    @Test
    void LocalDateTime_UTC_로_입력_받을수_있다() throws Exception {
        // given
        String cursor = "2025-09-16T12:34:56";

        // when
        Instant actual = invokeParse(cursor);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(Instant.parse("2025-09-16T12:34:56Z"));
    }

    @Test
    void cursor가_날짜가_아니면_null_반환() throws Exception {
        // given
        String[] bads = {
            "not-a-date",
            "2025-13-40T99:99:99Z",
            "2025/09/16 12:34:56"
        };

        for (String bad : bads) {
            // when
            Instant actual = invokeParse(bad);
            // then
            assertThat(actual).isNull();
        }
    }
    @Test
    void blank_null_커서는_null_반환() throws Exception {
        assertThat(invokeParse("")).isNull();
        assertThat(invokeParse("   ")).isNull();
        assertThat(invokeParse(null)).isNull();
    }


    /*
        팔로워 목록 조회 단위 테스트
     */

    @Test
    void 팔로워_목록_조회를_한다() throws Exception {
        // given
        UUID followeeId = UUID.randomUUID();
        FollowingRequest req = new FollowingRequest(followeeId, null, null, 10, null);

        User followee = mock(User.class, Answers.RETURNS_DEFAULTS);
        given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
        lenient().when(followee.isLocked()).thenReturn(false);

        Follow f1 = mock(Follow.class, Answers.RETURNS_DEFAULTS);
        Follow f2 = mock(Follow.class, Answers.RETURNS_DEFAULTS);
        given(followRepository.findFollowers(any(FollowingRequest.class)))
            .willReturn(List.of(f1, f2));

        given(followRepository.countTotalFollowers(followeeId, null)).willReturn(2L);

        given(followMapper.toDto(any(Follow.class))).willAnswer(inv ->
            new FollowDto(
                UUID.randomUUID(),
                new AuthorDto(followeeId, "followee", null),
                new AuthorDto(UUID.randomUUID(), "follower", null)
            )
        );

        // when
        FollowListResponse res = followService.getFollowers(req);

        // then
        assertThat(res).isNotNull();
        assertThat(res.data()).isNotEmpty();
    }

    @Test
    void 팔로워_조회시_유저가_있는지_확인한다() throws Exception {
        /// given
        UUID followerId = UUID.randomUUID();
        FollowingRequest req = new FollowingRequest(followerId, null, null, 10, null);

        given(userRepository.findById(followerId)).willReturn(Optional.empty());

        // when n then
        assertThatThrownBy(() -> followService.getFollowers(req))
            .isInstanceOf(OtbooException.class);
    }

    @Test
    void 팔로워_조회시_유저의_locked_여부를_확인한다() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        FollowingRequest req = new FollowingRequest(followerId, null, null, 10, null);

        User follower = mock(User.class, Answers.RETURNS_DEFAULTS);
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(follower.isLocked()).willReturn(true); // 잠금

        // when n then
        assertThatThrownBy(() -> followService.getFollowers(req))
            .isInstanceOf(OtbooException.class);
    }

    /*
        언팔로우 단위 테스트
     */

    @Test
    void 팔로우_여부_확인한다_실패() throws Exception {
        // given
        UUID followId = UUID.randomUUID();
        given(followRepository.existsById(followId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> followService.unfollow(followId))
            .isInstanceOf(OtbooException.class);

        then(followRepository).should(times(1)).existsById(followId);
        then(followRepository).should(never()).deleteById(any());
        then(followRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void 팔로우_취소한다() throws Exception {
        // given
        UUID followId = UUID.randomUUID();
        given(followRepository.existsById(followId)).willReturn(true);

        // when
        followService.unfollow(followId);

        // then
        then(followRepository).should(times(1)).deleteById(followId);
        then(followRepository).shouldHaveNoMoreInteractions();

    }
}