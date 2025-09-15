package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.user.UserSummaryDto; // 임시
import com.samsamotot.otboo.follow.entity.Follow;

import com.samsamotot.otboo.user.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;
/**
 * PackageName  : com.samsamotot.otboo.follow.fixture
 * FileName     : FollowFixture
 * Author       : dounguk
 * Date         : 2025. 9. 15.
 */

public final class FollowFixture {

    private FollowFixture() {}

    private static final PasswordEncoder DEFAULT_ENCODER = new BCryptPasswordEncoder();

    private static String randEmail() {
        return "user+" + UUID.randomUUID() + "@test.com";
    }

    private static String randName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // User 생성
    public static User randomUser() {
        return User.createUser(randEmail(), randName("tester"), "Password#1", DEFAULT_ENCODER);
    }

    public static User randomAdmin() {
        return User.createAdminUser(randEmail(), randName("admin"), "Password#1", DEFAULT_ENCODER);
    }

    public static User lockedUser() {
        User u = randomUser();
        u.changeLockStatus(true);
        return u;
    }

    // Follow 엔티티 생성
    public static Follow create(User follower, User followee) {
        return Follow.builder()
            .follower(follower)
            .followee(followee)
            .build();
    }

    // 랜덤 유저 2명으로 Follow 생성
    public static Follow createRandom() {
        User follower = randomUser();
        User followee = randomUser();
        return create(follower, followee);
    }

    // 자기 자신을 팔로우 invalid
    public static Follow createSelfFollow(User user) {
        return Follow.builder()
            .follower(user)
            .followee(user)
            .build();
    }

    // follower가 잠긴 상태 invalid
    public static Follow createWithLockedFollower(User followee) {
        return create(lockedUser(), followee);
    }

    // createdAt 주입
    public static Follow createWithCreatedAt(User follower, User followee, Instant createdAt) {
        Follow f = create(follower, followee);
        ReflectionTestUtils.setField(f, "createdAt", createdAt, Instant.class);
        return f;
    }

    // id 주입
    public static Follow createWithId(UUID id, User follower, User followee) {
        Follow f = create(follower, followee);
        ReflectionTestUtils.setField(f, "id", id, UUID.class);
        return f;
    }

    // Request/DTO 생성
    public static FollowCreateRequest createRequest(User follower, User followee) {
        return new FollowCreateRequest(follower.getId(), followee.getId());
    }

    public static FollowCreateRequest createRequest(UUID followerId, UUID followeeId) {
        return new FollowCreateRequest(followerId, followeeId);
    }

    public static FollowDto createDto(Follow follow) {
        User followee = follow.getFollowee();
        User follower = follow.getFollower();

        UserSummaryDto followeeSummary = new UserSummaryDto(
            followee.getId(),
            followee.getUsername(),
            /* profileImageUrl */ null    // 필요하면 채워 넣으세요
        );

        UserSummaryDto followerSummary = new UserSummaryDto(
            follower.getId(),
            follower.getUsername(),
            /* profileImageUrl */ null
        );

        return new FollowDto(
            // FollowDto의 id가 Follow 엔티티 id를 사용한다는 가정
            (UUID) ReflectionTestUtils.getField(follow, "id"),
            followeeSummary,
            followerSummary
        );
    }

    // 랜덤 유저 2명 + Request 한번에 생성
    public static class Pair {
        public final User follower;
        public final User followee;
        public Pair(User follower, User followee) { this.follower = follower; this.followee = followee; }
    }

    public static Pair randomUsers() {
        return new Pair(randomUser(), randomUser());
    }

    public static FollowCreateRequest randomRequest() {
        Pair p = randomUsers();
        return createRequest(p.follower, p.followee);
    }
}
