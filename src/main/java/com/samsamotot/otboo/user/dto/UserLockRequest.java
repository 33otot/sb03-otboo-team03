package com.samsamotot.otboo.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 계정 잠금 요청 DTO
 * 
 * <p>관리자가 사용자 계정의 잠금 상태를 변경할 때 사용되는 데이터 전송 객체입니다.</p>
 */
public record UserLockRequest(
    @NotNull(message = "잠금 상태는 필수입니다.")
    @JsonProperty("locked")
    Boolean locked
) {
}
