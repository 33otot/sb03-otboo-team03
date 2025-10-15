package com.samsamotot.otboo.user.repository;

import com.samsamotot.otboo.user.entity.Provider;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.custom.UserRepositoryCustom;
import io.micrometer.core.instrument.config.validate.Validated;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByEmailAndIsLocked(String email, Boolean locked);

    List<User> findByRole(Role role);

    List<User> findByIsLocked(Boolean locked);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
    
    /**
     * 이메일 패턴으로 사용자 검색(관리자-사용자 관리의 이메일 검색)
     */
    @Query("SELECT u FROM User u WHERE u.email LIKE %:emailPattern%")
    List<User> findByEmailContaining(@Param("emailPattern") String emailPattern);
    
    /**
     * 이름 패턴으로 사용자 검색(프로필 내 팔로워, 팔로일 검색)
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:namePattern%")
    List<User> findByUsernameContaining(@Param("namePattern") String namePattern);
    
    /**
     * 권한과 잠금 상태로 사용자 조회(관리자 - 사용자 관리의 권한과 잠금 상태로 사용자 조회)
     */
    List<User> findByRoleAndIsLocked(Role role, Boolean locked);
    
    /**
     * 임시 비밀번호가 만료된 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.temporaryPasswordExpiresAt IS NOT NULL AND u.temporaryPasswordExpiresAt < :now")
    List<User> findExpiredTemporaryPasswordUsers(@Param("now") Instant now);
    
    /**
     * 권한별 사용자 수 조회
     */
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countByRole();
    
    /**
     * 잠금 상태별 사용자 수 조회
     */
    @Query("SELECT u.isLocked, COUNT(u) FROM User u GROUP BY u.isLocked")
    List<Object[]> countByIsLocked();
    
    /**
     * 활성 사용자 ID 조회 (잠금되지 않은 사용자만)
     */
    @Query("SELECT u.id FROM User u WHERE u.isLocked = false")
    List<UUID> findActiveUserIds();
}
