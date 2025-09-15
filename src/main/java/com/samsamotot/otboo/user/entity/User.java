package com.samsamotot.otboo.user.entity;

import com.samsamotot.otboo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * 사용자 엔티티
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "temporary_password_expires_at")
    private Instant temporaryPasswordExpiresAt;
    
    @Builder
    private User(String email, String username, String password, Role role,
                Boolean isLocked, Instant temporaryPasswordExpiresAt) {
    this.email = email;
    this.username = username;
    this.password = password;
    this.role = role != null ? role : Role.USER;
    this.isLocked = isLocked != null ? isLocked : false;
    this.temporaryPasswordExpiresAt = temporaryPasswordExpiresAt;
    }
    
    public static User createUser(String email, String username, String password, PasswordEncoder passwordEncoder) {
        return User.builder()
            .email(email)
            .username(username)
            .password(passwordEncoder.encode(password))
            .role(Role.USER)
            .isLocked(false)
            .build();
    }
    
    public static User createAdminUser(String email, String username, String password, PasswordEncoder passwordEncoder) {
        return User.builder()
            .email(email)
            .username(username)
            .password(passwordEncoder.encode(password))
            .role(Role.ADMIN)
            .isLocked(false)
            .build();
    }

    public boolean checkPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, this.password);
    }
    
    public void changePassword(String newPassword, PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(newPassword);
        // 비밀번호 변경 시 임시 비밀번호 만료 시간 초기화
        this.temporaryPasswordExpiresAt = null;
    }
    
    public void setTemporaryPassword(String temporaryPassword, PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(temporaryPassword);
        this.temporaryPasswordExpiresAt = Instant.now().plusSeconds(3 * 60); // 3분 후 만료
    }

    public boolean isTemporaryPasswordValid() {
        return temporaryPasswordExpiresAt != null && 
            Instant.now().isBefore(temporaryPasswordExpiresAt);
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    public void changeLockStatus(boolean locked) {
        this.isLocked = locked;
    }

    public boolean isLocked() {
        return Boolean.TRUE.equals(this.isLocked);
    }

    public void updateUserInfo(String username) {
        this.username = username;
    }
}
