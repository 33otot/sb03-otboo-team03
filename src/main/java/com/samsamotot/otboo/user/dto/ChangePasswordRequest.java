package com.samsamotot.otboo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

/**
 * 비밀번호 변경 요청 DTO
 */
@Builder
public record ChangePasswordRequest(
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다."
    )
    String password
) {
}
