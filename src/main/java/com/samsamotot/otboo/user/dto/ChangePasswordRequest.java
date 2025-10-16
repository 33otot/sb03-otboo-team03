package com.samsamotot.otboo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 비밀번호 변경 요청 DTO
 */
@Builder
public record ChangePasswordRequest(
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
        message = "비밀번호는 영문과 숫자를 포함해 6자 이상으로 설정해야 합니다"
    )
    String password
) {
}
