package com.samsamotot.otboo.user.service;

import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

/**
 * 사용자 서비스 인터페이스
 */
public interface UserService {
    
    /**
     * 사용자 회원가입
     * @param request 회원가입 요청 DTO
     * @return 생성된 사용자 정보
     */
    UserDto createUser(UserCreateRequest request);
    
    /**
     * Spring Security를 위한 사용자 조회
     * @param userId 사용자 ID (문자열)
     * @return UserDetails 객체
     */
    UserDetails loadUserByUsername(String userId);
    
    /**
     * 사용자 ID로 사용자 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    UserDto getUserById(UUID userId);
}
