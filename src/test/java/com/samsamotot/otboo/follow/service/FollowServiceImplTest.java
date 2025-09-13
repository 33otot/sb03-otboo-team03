package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.N;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * PackageName  : com.samsamotot.otboo.follow.service
 * FileName     : FollowServiceImplTest
 * Author       : dounguk
 * Date         : 2025. 9. 13.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Follow service unit test")
class FollowServiceImplTest {
    @InjectMocks
    private FollowServiceImpl followService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    static final String EMAIL = "test@test.com";
    static final String NAME = "name";
    static final String PASSWORD = "password";
    static final Role ROLE = Role.USER;
    static final boolean LOCKED = false;
    static final Instant TEMPORARY_PASSWORD_EXPIRESAT = Instant.now().plusSeconds(60*10);

    @Test
    void 팔로우_생성한다() throws Exception {
        // given
        User follower = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);
        User followee = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);

        FollowCreateRequest validRequest = new FollowCreateRequest(follower.getId(), followee.getId());

        given(userRepository.findById(followee.getId()))
            .willReturn(Optional.of(followee));

        given(userRepository.findById(follower.getId()))
            .willReturn(Optional.of(follower));

        given(followRepository.existsFollowByFollowerIdAndFolloweeId(follower,followee))
            .willReturn(false);

        // when
        FollowDto follow = followService.follow(validRequest);

        // then
        assertNotNull(follow);
        assertThat(follow.follower().getId()).isEqualTo(follower.getId());
        assertThat(follow.followee().getId()).isEqualTo(followee.getId());
    }

    @Test
    void 두_유저가_팔로우_되어있는지_조회한다_실패() throws Exception {
        // given
        User follower = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);
        User followee = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);

        FollowCreateRequest invalidRequest = new FollowCreateRequest(follower.getId(), followee.getId());

        given(userRepository.findById(followee.getId()))
            .willReturn(Optional.of(followee));

        given(userRepository.findById(follower.getId()))
            .willReturn(Optional.of(follower));

        given(followRepository.existsFollowByFollowerIdAndFolloweeId(follower,followee))
            .willReturn(false);

        // when
        assertThatThrownBy(() -> followService.follow(invalidRequest))
            .isInstanceOf(Exception.class);

        // then
        then(followRepository).should().existsFollowByFollowerIdAndFolloweeId(follower,followee);
    }

    @Test
    void 유저가_있는지_확인한다_실패() throws Exception {
        // given
        User follower = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);
        User followee = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);

        FollowCreateRequest invalidRequest = new FollowCreateRequest(follower.getId(), followee.getId());

        given(userRepository.findById(followee.getId()))
            .willReturn(null);

        given(userRepository.findById(follower.getId()))
            .willReturn(null);

        given(followRepository.existsFollowByFollowerIdAndFolloweeId(follower,followee))
            .willReturn(true);

        // when
        assertThatThrownBy(() -> followService.follow(invalidRequest))
            .isInstanceOf(Exception.class);

        // then
        then(followRepository).shouldHaveNoInteractions();
        then(userRepository).should().findById(followee.getId());
        then(userRepository).should().findById(follower.getId());
    }

    @Test
    void follower_논리삭제_여부_확인한다_실패() throws Exception {
        // given
        User follower = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);
        ReflectionTestUtils.setField(follower, "locked", true);

        User followee = User.createUser(EMAIL,NAME,PASSWORD,PASSWORD_ENCODER);

        FollowCreateRequest invalidRequest = new FollowCreateRequest(follower.getId(), followee.getId());

        given(userRepository.findById(followee.getId()))
            .willReturn(Optional.of(followee));

        given(userRepository.findById(follower.getId()))
            .willReturn(Optional.of(follower));

        given(followRepository.existsFollowByFollowerIdAndFolloweeId(follower,followee))
            .willReturn(false);

        // when
        assertThatThrownBy(() -> followService.follow(invalidRequest))
            .isInstanceOf(Exception.class);

        // then
        then(followRepository).shouldHaveNoInteractions();
    }
}