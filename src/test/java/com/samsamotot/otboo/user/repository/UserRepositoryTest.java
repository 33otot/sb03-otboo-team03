package com.samsamotot.otboo.user.repository;

import com.samsamotot.otboo.common.fixture.UserFixture;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository 단위 테스트")
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = UserFixture.createValidUser();
    }

    @Test
    @DisplayName("사용자를_저장하고_조회하면_정상적으로_작동한다")
    void 사용자를_저장하고_조회하면_정상적으로_작동한다() {
        // given
        String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";
        User user = UserFixture.createUserWithEmail(uniqueEmail);
        UUID userId = UUID.randomUUID();

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        User savedUser = userRepository.save(user);
        Optional<User> foundUser = userRepository.findById(userId);

        // then
        assertThat(savedUser).isNotNull();
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(uniqueEmail);
    }

    @Test
    @DisplayName("이메일로_사용자를_조회하면_해당_사용자를_반환한다")
    void 이메일로_사용자를_조회하면_해당_사용자를_반환한다() {
        // given
        String uniqueEmail = "email-test-" + UUID.randomUUID() + "@example.com";
        User user = UserFixture.createUserWithEmail(uniqueEmail);

        when(userRepository.findByEmail(uniqueEmail)).thenReturn(Optional.of(user));

        // when
        Optional<User> foundUser = userRepository.findByEmail(uniqueEmail);

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(uniqueEmail);
    }

    @Test
    @DisplayName("존재하지_않는_이메일로_조회하면_빈_Optional을_반환한다")
    void 존재하지_않는_이메일로_조회하면_빈_Optional을_반환한다() {
        // given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // when
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("이메일_중복_검사시_존재하는_이메일이면_true를_반환한다")
    void 이메일_중복_검사시_존재하는_이메일이면_true를_반환한다() {
        // given
        String uniqueEmail = "duplicate-test-" + UUID.randomUUID() + "@example.com";
        when(userRepository.existsByEmail(uniqueEmail)).thenReturn(true);

        // when
        boolean exists = userRepository.existsByEmail(uniqueEmail);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("이메일_중복_검사시_존재하지_않는_이메일이면_false를_반환한다")
    void 이메일_중복_검사시_존재하지_않는_이메일이면_false를_반환한다() {
        // given
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        // when
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("이메일과_잠금상태로_사용자를_조회하면_해당_사용자를_반환한다")
    void 이메일과_잠금상태로_사용자를_조회하면_해당_사용자를_반환한다() {
        // given
        String uniqueEmail = "email-lock-test-" + UUID.randomUUID() + "@example.com";
        User user = UserFixture.createUserWithEmail(uniqueEmail);

        when(userRepository.findByEmailAndIsLocked(uniqueEmail, false)).thenReturn(Optional.of(user));

        // when
        Optional<User> foundUser = userRepository.findByEmailAndIsLocked(uniqueEmail, false);

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(uniqueEmail);
        assertThat(foundUser.get().isLocked()).isFalse();
    }

    @Test
    @DisplayName("권한으로_사용자_목록을_조회하면_해당_권한의_사용자들을_반환한다")
    void 권한으로_사용자_목록을_조회하면_해당_권한의_사용자들을_반환한다() {
        // given
        String userEmail = "role-user-" + UUID.randomUUID() + "@example.com";
        String adminEmail = "role-admin-" + UUID.randomUUID() + "@example.com";
        
        User user = UserFixture.createUserWithEmail(userEmail);
        User adminUser = UserFixture.createUserWithEmail(adminEmail);
        adminUser.changeRole(Role.ADMIN);

        when(userRepository.findByRole(Role.USER)).thenReturn(List.of(user));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(adminUser));

        // when
        List<User> userRoleUsers = userRepository.findByRole(Role.USER);
        List<User> adminRoleUsers = userRepository.findByRole(Role.ADMIN);

        // then
        assertThat(userRoleUsers).hasSize(1);
        assertThat(userRoleUsers.get(0).getRole()).isEqualTo(Role.USER);
        
        assertThat(adminRoleUsers).hasSize(1);
        assertThat(adminRoleUsers.get(0).getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("잠금상태로_사용자_목록을_조회하면_해당_상태의_사용자들을_반환한다")
    void 잠금상태로_사용자_목록을_조회하면_해당_상태의_사용자들을_반환한다() {
        // given
        String unlockedEmail = "unlocked-" + UUID.randomUUID() + "@example.com";
        String lockedEmail = "locked-" + UUID.randomUUID() + "@example.com";
        
        User unlockedUser = UserFixture.createUserWithEmail(unlockedEmail);
        User lockedUser = UserFixture.createUserWithEmail(lockedEmail);
        lockedUser.changeLockStatus(true);

        when(userRepository.findByIsLocked(false)).thenReturn(List.of(unlockedUser));
        when(userRepository.findByIsLocked(true)).thenReturn(List.of(lockedUser));

        // when
        List<User> unlockedUsers = userRepository.findByIsLocked(false);
        List<User> lockedUsers = userRepository.findByIsLocked(true);

        // then
        assertThat(unlockedUsers).hasSize(1);
        assertThat(unlockedUsers.get(0).isLocked()).isFalse();
        
        assertThat(lockedUsers).hasSize(1);
        assertThat(lockedUsers.get(0).isLocked()).isTrue();
    }

    @Test
    @DisplayName("이메일_패턴으로_검색하면_해당_패턴의_사용자들을_반환한다")
    void 이메일_패턴으로_검색하면_해당_패턴의_사용자들을_반환한다() {
        // given
        String pattern = "search-" + UUID.randomUUID().toString().substring(0, 8);
        String email1 = pattern + "1@example.com";
        String email2 = pattern + "2@example.com";
        
        User user1 = UserFixture.createUserWithEmail(email1);
        User user2 = UserFixture.createUserWithEmail(email2);

        when(userRepository.findByEmailContaining(pattern)).thenReturn(List.of(user1, user2));

        // when
        List<User> foundUsers = userRepository.findByEmailContaining(pattern);

        // then
        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers).extracting(User::getEmail)
            .containsExactlyInAnyOrder(email1, email2);
    }

    @Test
    @DisplayName("이름_패턴으로_검색하면_해당_패턴의_사용자들을_반환한다")
    void 이름_패턴으로_검색하면_해당_패턴의_사용자들을_반환한다() {
        // given
        String pattern = "검색테스트" + UUID.randomUUID().toString().substring(0, 4);
        String email1 = "name1-" + UUID.randomUUID() + "@example.com";
        String email2 = "name2-" + UUID.randomUUID() + "@example.com";
        
        User user1 = UserFixture.createUserWithEmail(email1);
        user1.updateUserInfo(pattern + "1");
        User user2 = UserFixture.createUserWithEmail(email2);
        user2.updateUserInfo(pattern + "2");

        when(userRepository.findByUsernameContaining(pattern)).thenReturn(List.of(user1, user2));

        // when
        List<User> foundUsers = userRepository.findByUsernameContaining(pattern);

        // then
        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers).extracting(User::getUsername)
            .containsExactlyInAnyOrder(pattern + "1", pattern + "2");
    }

    @Test
    @DisplayName("권한과_잠금상태로_사용자를_조회하면_해당_조건의_사용자들을_반환한다")
    void 권한과_잠금상태로_사용자를_조회하면_해당_조건의_사용자들을_반환한다() {
        // given
        String userEmail1 = "complex-user1-" + UUID.randomUUID() + "@example.com";
        String userEmail2 = "complex-user2-" + UUID.randomUUID() + "@example.com";
        String adminEmail = "complex-admin-" + UUID.randomUUID() + "@example.com";
        
        User user1 = UserFixture.createUserWithEmail(userEmail1);
        User user2 = UserFixture.createUserWithEmail(userEmail2);
        user2.changeLockStatus(true);
        User admin = UserFixture.createUserWithEmail(adminEmail);
        admin.changeRole(Role.ADMIN);

        when(userRepository.findByRoleAndIsLocked(Role.USER, false)).thenReturn(List.of(user1));
        when(userRepository.findByRoleAndIsLocked(Role.USER, true)).thenReturn(List.of(user2));
        when(userRepository.findByRoleAndIsLocked(Role.ADMIN, false)).thenReturn(List.of(admin));

        // when
        List<User> unlockedUsers = userRepository.findByRoleAndIsLocked(Role.USER, false);
        List<User> lockedUsers = userRepository.findByRoleAndIsLocked(Role.USER, true);
        List<User> unlockedAdmins = userRepository.findByRoleAndIsLocked(Role.ADMIN, false);

        // then
        assertThat(unlockedUsers).hasSize(1);
        assertThat(unlockedUsers.get(0).getRole()).isEqualTo(Role.USER);
        assertThat(unlockedUsers.get(0).isLocked()).isFalse();
        
        assertThat(lockedUsers).hasSize(1);
        assertThat(lockedUsers.get(0).getRole()).isEqualTo(Role.USER);
        assertThat(lockedUsers.get(0).isLocked()).isTrue();
        
        assertThat(unlockedAdmins).hasSize(1);
        assertThat(unlockedAdmins.get(0).getRole()).isEqualTo(Role.ADMIN);
        assertThat(unlockedAdmins.get(0).isLocked()).isFalse();
    }

    @Test
    @DisplayName("만료된_임시_비밀번호_사용자를_조회하면_해당_사용자들을_반환한다")
    void 만료된_임시_비밀번호_사용자를_조회하면_해당_사용자들을_반환한다() {
        // given
        String email1 = "temp1-" + UUID.randomUUID() + "@example.com";
        String email2 = "temp2-" + UUID.randomUUID() + "@example.com";
        
        User user1 = UserFixture.createUserWithEmail(email1);
        User user2 = UserFixture.createUserWithEmail(email2);

        when(userRepository.findExpiredTemporaryPasswordUsers(any(Instant.class))).thenReturn(List.of(user1, user2));

        // when
        List<User> expiredUsers = userRepository.findExpiredTemporaryPasswordUsers(Instant.now().plusSeconds(3600));

        // then
        assertThat(expiredUsers).hasSize(2);
    }

    @Test
    @DisplayName("권한별_사용자_수를_조회하면_올바른_통계를_반환한다")
    void 권한별_사용자_수를_조회하면_올바른_통계를_반환한다() {
        // given
        Object[] userCount = {Role.USER, 2L};
        Object[] adminCount = {Role.ADMIN, 1L};
        List<Object[]> roleCounts = List.of(userCount, adminCount);

        when(userRepository.countByRole()).thenReturn(roleCounts);

        // when
        List<Object[]> result = userRepository.countByRole();

        // then
        assertThat(result).hasSize(2);
        
        // Role과 Count를 확인
        boolean hasUserRole = result.stream()
            .anyMatch(count -> count[0].equals(Role.USER) && count[1].equals(2L));
        boolean hasAdminRole = result.stream()
            .anyMatch(count -> count[0].equals(Role.ADMIN) && count[1].equals(1L));
        
        assertThat(hasUserRole).isTrue();
        assertThat(hasAdminRole).isTrue();
    }

    @Test
    @DisplayName("잠금상태별_사용자_수를_조회하면_올바른_통계를_반환한다")
    void 잠금상태별_사용자_수를_조회하면_올바른_통계를_반환한다() {
        // given
        Object[] unlockedCount = {false, 2L};
        Object[] lockedCount = {true, 1L};
        List<Object[]> lockCounts = List.of(unlockedCount, lockedCount);

        when(userRepository.countByIsLocked()).thenReturn(lockCounts);

        // when
        List<Object[]> result = userRepository.countByIsLocked();

        // then
        assertThat(result).hasSize(2);
        
        // 잠금 상태와 Count를 확인
        boolean hasUnlocked = result.stream()
            .anyMatch(count -> count[0].equals(false) && count[1].equals(2L));
        boolean hasLocked = result.stream()
            .anyMatch(count -> count[0].equals(true) && count[1].equals(1L));
        
        assertThat(hasUnlocked).isTrue();
        assertThat(hasLocked).isTrue();
    }
}