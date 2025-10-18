package com.samsamotot.otboo.auth.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.samsamotot.otboo.auth.dto.ResetPasswordRequest;
import com.samsamotot.otboo.common.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 인증 관리 API 인터페이스
 */
@Tag(name = "인증 관리", description = "인증 관련 API")
public interface AuthApi {

    @Operation(
        summary = "비밀번호 초기화",
        description = "임시 비밀번호로 초기화 후 이메일로 전송합니다.",
        operationId = "resetPassword"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "204",
                description = "비밀번호 초기화 성공"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "비밀번호 초기화 실패(사용자 없음)",
                content = @Content(
                    mediaType = "*/*",
                    schema = @Schema(implementation = ErrorResponse.class)
                )
            )
        }
    )
    ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request);
}
