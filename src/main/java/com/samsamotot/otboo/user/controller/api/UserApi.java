package com.samsamotot.otboo.user.controller.api;

import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.profile.dto.ProfileDto;
import com.samsamotot.otboo.profile.dto.ProfileUpdateRequest;
import com.samsamotot.otboo.user.dto.UserCreateRequest;
import com.samsamotot.otboo.user.dto.UserDto;
import com.samsamotot.otboo.user.dto.UserDtoCursorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
            @RequestPart(value = "image", required = false) MultipartFile image
    );

    @Operation(
        summary = "계정 목록 조회",
        description = "계정 목록 조회 API",
        operationId = "getUserList"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "계정 목록 조회 성공",
                content = @Content(mediaType = "*/*", schema = @Schema(implementation = UserDtoCursorResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "계정 목록 조회 실패",
                content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class))
            )
        }
    )
    ResponseEntity<UserDtoCursorResponse> getUserList(
        @Parameter(description = "페이지 크기") int limit,
        @Parameter(description = "정렬 기준") String sortBy,
        @Parameter(description = "정렬 방향") String sortDirection,
        @Parameter(description = "페이지네이션 커서") String cursor,
        @Parameter(description = "보조 커서") UUID idAfter,
        @Parameter(description = "이메일 검색") String emailLike,
        @Parameter(description = "권한 필터") String roleEqual,
        @Parameter(description = "잠금 상태 필터") Boolean locked
    );
}
