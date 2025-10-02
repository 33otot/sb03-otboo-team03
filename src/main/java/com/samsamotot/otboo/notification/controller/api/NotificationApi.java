package com.samsamotot.otboo.notification.controller.api;

import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.notification.dto.NotificationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.controller.api
 * FileName     : NotificationApi
 * Author       : dounguk
 * Date         : 2025. 10. 1.
 */

@Tag(name = "알림", description = "알림 API")
public interface NotificationApi {

    @Operation(summary = "알림 목록 조회",description = "알림 목록 조회 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = NotificationListResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "알림 목록 조회 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping
    ResponseEntity<NotificationListResponse> getNotifications(
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        Integer limit
    );

    @Operation(summary = "알림 읽음 처리", description = "알림 읽음 처리 API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "알림 읽음 처리 성공"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "알림 읽음 처리 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @DeleteMapping
    ResponseEntity<Void> deleteNotifications(@PathVariable UUID notificationId);
}
