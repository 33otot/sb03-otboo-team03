package com.samsamotot.otboo.user.controller.api;

import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

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

    @Operation(
            summary = "프로필 조회",
            description = "프로필 조회 API",
            operationId = "getProfile"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프로필 조회 성공",
                            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ProfileDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로필 조회 실패",
                            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<ProfileDto> getProfile(@PathVariable UUID userId);

    @Operation(
            summary = "프로필 수정",
            description = "프로필 수정 API",
            operationId = "updateProfile"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프로필 수정 성공",
                            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ProfileDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로필 수정 실패",
                            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<ProfileDto> updateProfile(
            @PathVariable UUID userId,
            @RequestPart("request") @Valid ProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    );

}
