package com.samsamotot.otboo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그아웃 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    
    /**
     * 리프레시 토큰
     */
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}
