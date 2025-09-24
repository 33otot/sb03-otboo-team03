package com.samsamotot.otboo.common.security.jwt;

import com.samsamotot.otboo.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JWT 인증 응답을 위한 데이터 전송 객체 (DTO)
 * 
 * <p>이 클래스는 JWT 기반 인증 성공 시 클라이언트에게 반환되는 응답 데이터를
 * 캡슐화합니다. 사용자 정보, 액세스 토큰, 리프레시 토큰 등의 인증 관련 정보를
 * 포함하여 프론트엔드에서 인증 상태를 관리할 수 있도록 합니다.</p>
 * 
 * <h3>주요 필드:</h3>
 * <ul>
 *   <li><strong>userDto</strong>: 인증된 사용자의 기본 정보 (ID, 이메일, 이름 등)</li>
 *   <li><strong>accessToken</strong>: API 접근을 위한 JWT 액세스 토큰 (1시간 유효)</li>
 *   <li><strong>refreshToken</strong>: 액세스 토큰 갱신을 위한 JWT 리프레시 토큰 (7일 유효)</li>
 *   <li><strong>tokenType</strong>: 토큰 타입 (기본값: "Bearer")</li>
 *   <li><strong>expiresIn</strong>: 액세스 토큰의 만료 시간 (초 단위)</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtDto {
    
    /**
     * 사용자 정보
     */
    private UserDto userDto;
    
    /**
     * 액세스 토큰
     */
    private String accessToken;
    
    /**
     * 리프레시 토큰
     */
    private String refreshToken;
    
    /**
     * 토큰 타입 (Bearer)
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 액세스 토큰 만료 시간 (초)
     */
    private Long expiresIn;
}
