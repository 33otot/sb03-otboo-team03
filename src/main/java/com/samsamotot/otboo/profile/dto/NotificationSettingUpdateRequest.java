package com.samsamotot.otboo.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "날씨 알림 설정 업데이트 요청 DTO")
public record NotificationSettingUpdateRequest(
        @NotNull
        @Schema(description = "날씨 알림 수신 여부", example = "true")
        boolean weatherNotificationEnabled
) {
}
