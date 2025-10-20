package com.samsamotot.otboo.directmessage.controller.api;

import com.samsamotot.otboo.directmessage.dto.DirectMessageListResponse;
import com.samsamotot.otboo.directmessage.dto.MessageRequest;
import com.samsamotot.otboo.directmessage.service.DirectMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.samsamotot.otboo.directmessage.dto.DirectMessageRoomListResponse; // Added
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Added
import com.samsamotot.otboo.common.security.service.CustomUserDetails; // Added

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.controller.api
 * FileName     : DirectMessageApi
 * Author       : dounguk
 * Date         : 2025. 9. 25.
 */
@Tag(name = "DM 목록 조회",description = "DM 목록 조회 API" )
public interface DirectMessageApi {

    @Operation(summary = "DM 목록 조회")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "DM 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DirectMessageListResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "DM 목록 조회 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    }
    )
    @GetMapping
    ResponseEntity<DirectMessageListResponse> directMessages(
        @RequestParam UUID userId,
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit
    );

    @Operation(summary = "대화방 목록 조회", description = "현재 사용자의 모든 대화방 목록과 각 대화방의 마지막 메시지 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "대화방 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DirectMessageRoomListResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/rooms")
    ResponseEntity<DirectMessageRoomListResponse> getConversationRooms(
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
