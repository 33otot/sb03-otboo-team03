package com.samsamotot.otboo.user.dto;

import com.samsamotot.otboo.user.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 권한 수정 요청 DTO
 * 
 * <p>관리자가 사용자의 권한을 변경할 때 사용되는 데이터 전송 객체입니다.</p>
 */
public record UserRoleUpdateRequest(
    @NotNull(message = "권한은 필수입니다.")
    Role role
) {
}
