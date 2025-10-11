package com.samsamotot.otboo.user.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.jwt.TokenInvalidationService;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.notification.dto.event.RoleChangedEvent;
import com.samsamotot.otboo.profile.entity.Profile;
import com.samsamotot.otboo.profile.repository.ProfileRepository;
import com.samsamotot.otboo.user.dto.*;
import com.samsamotot.otboo.user.entity.Provider;
import com.samsamotot.otboo.user.entity.Role;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.exception.DuplicateEmailException;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 사용자 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    
    private final String SERVICE = "[UserServiceImpl] ";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ProfileRepository profileRepository;
    private final TokenInvalidationService tokenInvalidationService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public UserDto createUser(UserCreateRequest request) {
        log.info(SERVICE + "회원가입 시도 - 이메일: {}", request.getEmail());
        
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn(SERVICE + "이메일 중복 - 이메일: {}", request.getEmail());
            throw new DuplicateEmailException(request.getEmail());
        }
        
        // User 엔티티 생성
        User user = User.builder()
            .email(request.getEmail())
            .username(request.getName())
            .password(passwordEncoder.encode(request.getPassword()))
            .provider(Provider.LOCAL)
            .providerId(null)
            .role(com.samsamotot.otboo.user.entity.Role.USER)
            .isLocked(false)
            .build();

        // 사용자 저장
        User savedUser = userRepository.save(user);

        // 사용자 기본 프로필 생성
        Profile userProfile = Profile.builder()
                .user(user)
                .name(user.getUsername())
                .build();

        // 사용자 프로필 저장
        profileRepository.save(userProfile);

        log.info(SERVICE + "회원가입 성공 - 사용자 ID: {}, 이메일: {}", savedUser.getId(), savedUser.getEmail());
        
        // DTO 변환하여 반환 (매퍼 사용)
        return userMapper.toDto(savedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) {
        log.debug(SERVICE + "사용자 조회 시도 - 사용자 ID: {}", userId);
        
        try {
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));
            
            log.debug(SERVICE + "사용자 조회 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
            return new CustomUserDetails(user);
            
        } catch (IllegalArgumentException e) {
            log.warn(SERVICE + "잘못된 사용자 ID 형식 - 사용자 ID: {}", userId);
            throw new OtbooException(ErrorCode.USER_NOT_FOUND);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        log.debug(SERVICE + "사용자 조회 시도 - 사용자 ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));
        
        log.debug(SERVICE + "사용자 조회 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDtoCursorResponse getUserList(UserListRequest request) {
        log.info(SERVICE + "사용자 목록 조회 시작 - 요청: {}", request);

        // Repository를 통한 사용자 목록 조회
        Slice<User> userSlice = userRepository.findUsersWithCursor(request);

        // 전체 사용자 수 조회
        long totalCount = userRepository.countUsersWithFilters(request);

        // User 엔티티를 UserDto로 변환
        List<UserDto> userDtos = userSlice.getContent().stream()
            .map(userMapper::toDto)
            .toList();

        // 다음 커서 생성
        String nextCursor = null;
        UUID nextIdAfter = null;

        if (userSlice.hasNext()) {
            User lastUser = userSlice.getContent().get(userSlice.getContent().size() - 1);
            log.info(SERVICE + "lastUser createdAt: {}", lastUser.getCreatedAt());
            if ("email".equals(request.sortBy())) {
                nextCursor = lastUser.getEmail();
            } else { // createdAt
                nextCursor = lastUser.getCreatedAt().toString();
            }
            nextIdAfter = lastUser.getId();
            log.info(SERVICE + "nextCursor: {}, nextIdAfter: {}", nextCursor, nextIdAfter);
        }

        log.info(SERVICE + "사용자 목록 조회 완료 - 조회된 수: {}, 전체 수: {}, hasNext: {}",
            userDtos.size(), totalCount, userSlice.hasNext());

        return new UserDtoCursorResponse(
            userDtos,
            nextCursor,
            nextIdAfter,
            userSlice.hasNext(),
            totalCount,
            request.sortBy(),
            request.sortDirection()
        );
    }
    
    @Override
    public UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        log.info(SERVICE + "권한 수정 시도 - 사용자 ID: {}, 새로운 권한: {}", userId, request.role());
        
        // 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn(SERVICE + "권한 수정 시 사용자 조회 실패 - 사용자 ID: {}", userId);
                return new OtbooException(ErrorCode.USER_NOT_FOUND);
            });
        
        // 현재 권한 저장
        Role previousRole = user.getRole();
        
        // 권한 변경
        user.changeRole(request.role());
        
        // 컷오프: 이 시점 이전 토큰 무효화
        tokenInvalidationService.setUserInvalidAfter(userId.toString(), java.time.Instant.now());
        
        log.info(SERVICE + "권한 수정 완료 - 사용자 ID: {}, 이전 권한: {}, 새로운 권한: {}", 
            userId, previousRole, request.role());

        // 권한 변경 알림 저장
        eventPublisher.publishEvent(new RoleChangedEvent(userId));
        
        // DTO 변환하여 반환
        return userMapper.toDto(user);
    }
}
