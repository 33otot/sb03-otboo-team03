package com.samsamotot.otboo.user.service.impl;

import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.exception.DuplicateEmailException;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.user.repository.UserRepository;
import com.samsamotot.otboo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    
    @Override
    public UserDto createUser(UserCreateRequest request) {
        log.info("회원가입 시도 - 이메일: {}", request.getEmail());
        
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("이메일 중복 - 이메일: {}", request.getEmail());
            throw new DuplicateEmailException(request.getEmail());
        }
        
        // User 엔티티 생성
        User user = User.builder()
            .email(request.getEmail())
            .username(request.getName())
            .password(passwordEncoder.encode(request.getPassword()))
            .provider("local")
            .providerId(null)
            .role(com.samsamotot.otboo.user.entity.Role.USER)
            .isLocked(false)
            .build();
        
        // 사용자 저장
        User savedUser = userRepository.save(user);
        
        log.info("회원가입 성공 - 사용자 ID: {}, 이메일: {}", savedUser.getId(), savedUser.getEmail());
        
        // DTO 변환하여 반환 (매퍼 사용)
        return userMapper.toDto(savedUser);
    }
}
