package com.samsamotot.otboo.user.controller.api;

import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * 사용자 API 인터페이스
 */
@Tag(name = "프로필 관리", description = "프로필 관련 API")
public interface UserApi {
    
    @Operation(
        summary = "사용자 등록(회원가입)",
        description = "사용자 등록(회원가입) API",
        operationId = "createUser"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "201",
                description = "사용자 등록(회원가입) 성공",
                content = @Content(mediaType = "*/*", schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "사용자 등록(회원가입) 실패",
                content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class))
            )
        }
    )
    ResponseEntity<UserDto> createUser(UserCreateRequest request);
}
