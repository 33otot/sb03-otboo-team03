package com.samsamotot.otboo.user.config;

import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AdminInitializer 테스트 클래스
 */
@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
    // 설정값 주입
    ReflectionTestUtils.setField(adminInitializer, "adminEmail", "admin@otboo.com");
    ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
    ReflectionTestUtils.setField(adminInitializer, "adminPassword", "admin123!");
    }

    @Test
    @DisplayName("관리자 계정이 존재하지 않을 때 새로 생성한다")
    void shouldCreateAdminUserWhenNotExists() throws Exception {
    // Given
    when(userRepository.findByRole(Role.ADMIN)).thenReturn(Collections.emptyList());
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(any(User.class));

    // When
    adminInitializer.run();

    // Then
    verify(userRepository).findByRole(Role.ADMIN);
    verify(passwordEncoder).encode("admin123!");
    verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("관리자 계정이 이미 존재할 때 생성하지 않는다")
    void shouldNotCreateAdminUserWhenExists() throws Exception {
    // Given
    User existingAdmin = User.builder()
        .email("admin@otboo.com")
        .username("admin")
        .password("encodedPassword")
        .role(Role.ADMIN)
        .build();

    when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(existingAdmin));

    // When
    adminInitializer.run();

    // Then
    verify(userRepository).findByRole(Role.ADMIN);
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("다른 이메일의 관리자가 존재해도 새로 생성한다")
    void shouldCreateAdminUserWhenDifferentEmailExists() throws Exception {
    // Given
    User existingAdmin = User.builder()
        .email("other@otboo.com")
        .username("other")
        .password("encodedPassword")
        .role(Role.ADMIN)
        .build();

    when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(existingAdmin));
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(any(User.class));

    // When
    adminInitializer.run();

    // Then
    verify(userRepository).findByRole(Role.ADMIN);
    verify(passwordEncoder).encode("admin123!");
    verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("예외 발생 시에도 애플리케이션이 중단되지 않는다")
    void shouldNotThrowExceptionWhenErrorOccurs() throws Exception {
    // Given
    when(userRepository.findByRole(Role.ADMIN)).thenThrow(new RuntimeException("Database error"));

    // When & Then
    assertThatCode(() -> adminInitializer.run())
        .doesNotThrowAnyException();
    }
}
