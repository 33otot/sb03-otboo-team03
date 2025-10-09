package com.samsamotot.otboo.user.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.samsamotot.otboo.common.security.jwt.TokenInvalidationService;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.dto.UserRoleUpdateRequest;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.user.service.impl.UserServiceImpl;

public class UserServiceImplCutoffTest {

  @Test
  @DisplayName("권한 변경 시 invalidAfter=now가 호출된다")
  void updateRole_setsInvalidAfterNow() {
    UserRepository userRepository = mock(UserRepository.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    UserMapper userMapper = mock(UserMapper.class);
    ProfileRepository profileRepository = mock(ProfileRepository.class);
    TokenInvalidationService tokenInvalidationService = mock(TokenInvalidationService.class);
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    UserServiceImpl sut = new UserServiceImpl(
        userRepository, passwordEncoder, userMapper, profileRepository, tokenInvalidationService, eventPublisher);

    UUID userId = UUID.randomUUID();
    User user = User.builder()
        .email("user@ex.com")
        .username("u")
        .password("p")
        .role(Role.USER)
        .isLocked(false)
        .provider(com.samsamotot.otboo.user.entity.Provider.LOCAL)
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(null);

    sut.updateUserRole(userId, new UserRoleUpdateRequest(Role.ADMIN));

    verify(tokenInvalidationService).setUserInvalidAfter(org.mockito.ArgumentMatchers.eq(userId.toString()),
        org.mockito.ArgumentMatchers.any());
  }
}


