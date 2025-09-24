package com.samsamotot.otboo.auth.service.impl;

import com.samsamotot.otboo.auth.dto.LoginRequest;
import com.samsamotot.otboo.auth.service.AuthService;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.security.csrf.CsrfTokenService;
import com.samsamotot.otboo.common.security.jwt.JwtDto;
import com.samsamotot.otboo.common.security.jwt.JwtTokenProvider;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.mapper.UserMapper;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 인증 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    
    private final String SERVICE = "[AuthServiceImpl] ";
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CsrfTokenService csrfTokenService;
    private final UserMapper userMapper;
    
    @Override
    public JwtDto login(LoginRequest request) {
        log.info(SERVICE + "로그인 시도 - 이메일: {}", request.getEmail());
        
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.warn(SERVICE + "존재하지 않는 이메일 - 이메일: {}", request.getEmail());
                return new OtbooException(ErrorCode.USER_NOT_FOUND);
            });
        
        // 계정 잠금 확인
        if (user.isLocked()) {
            log.warn(SERVICE + "잠긴 계정 로그인 시도 - 이메일: {}", request.getEmail());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }
        
        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn(SERVICE + "잘못된 비밀번호 - 이메일: {}", request.getEmail());
            throw new OtbooException(ErrorCode.EMAIL_OR_PASSWORD_MISMATCH);
        }
        
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        Long expirationEpoch = jwtTokenProvider.getExpirationTime(accessToken);
        Long expiresIn = Math.max(0, expirationEpoch - java.time.Instant.now().getEpochSecond());
        
        // 사용자 정보 DTO 변환
        UserDto userDto = userMapper.toDto(user);
        
        log.info(SERVICE + "로그인 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
        
        return JwtDto.builder()
            .userDto(userDto)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(expiresIn)
            .build();
    }
    
    @Override
    public void logout(String refreshToken) {
        String tokenInfo = (refreshToken != null && refreshToken.length() > 4) 
            ? "****" + refreshToken.substring(refreshToken.length() - 4)
            : "<redacted>";
        log.info(SERVICE + "로그아웃 시도 - 리프레시 토큰: {}", tokenInfo);
        
        // 리프레시 토큰 검증 (선택적)
        try {
            if (refreshToken != null) {
                jwtTokenProvider.validateToken(refreshToken);
            }
            log.info(SERVICE + "로그아웃 성공");
        } catch (Exception e) {
            log.warn(SERVICE + "유효하지 않은 리프레시 토큰으로 로그아웃 시도");
            // 로그아웃은 항상 성공으로 처리 (토큰이 이미 만료되었을 수 있음)
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getCsrfToken() {
        log.debug(SERVICE + "CSRF 토큰 조회");
        return csrfTokenService.generateCsrfToken();
    }
    
    @Override
    public JwtDto refreshToken(String refreshToken) {
        log.info(SERVICE + "토큰 갱신 시도");
        
        try {
            // 리프레시 토큰 검증
            jwtTokenProvider.validateToken(refreshToken);
            
            // 사용자 ID 추출
            UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            
            // 사용자 조회
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn(SERVICE + "토큰 갱신 시 사용자 조회 실패 - 사용자 ID: {}", userId);
                    return new OtbooException(ErrorCode.USER_NOT_FOUND);
                });
            
            // 계정 잠금 확인
            if (user.isLocked()) {
                log.warn(SERVICE + "잠긴 계정의 토큰 갱신 시도 - 사용자 ID: {}", userId);
                throw new OtbooException(ErrorCode.USER_LOCKED);
            }
            
            // 새로운 JWT 토큰 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(user.getId());
            String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
            Long expirationEpoch = jwtTokenProvider.getExpirationTime(newAccessToken);
            Long expiresIn = Math.max(0, expirationEpoch - java.time.Instant.now().getEpochSecond());
            
            // 사용자 정보 DTO 변환
            UserDto userDto = userMapper.toDto(user);
            
            log.info(SERVICE + "토큰 갱신 성공 - 사용자 ID: {}", userId);
            
            return JwtDto.builder()
                .userDto(userDto)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(expiresIn)
                .build();
                
        } catch (Exception e) {
            log.warn(SERVICE + "토큰 갱신 실패: {}", e.getMessage());
            throw new OtbooException(ErrorCode.TOKEN_INVALID);
        }
    }
}
