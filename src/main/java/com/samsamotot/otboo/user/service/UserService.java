package com.samsamotot.otboo.user.service;

import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;

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
}
