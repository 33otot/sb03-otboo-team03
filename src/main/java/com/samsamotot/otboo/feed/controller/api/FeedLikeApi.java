package com.samsamotot.otboo.feed.controller.api;

import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "피드 관리", description = "피드 관련 API")
public interface FeedLikeApi {

    @Operation(summary = "피드 좋아요")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "피드 좋아요 성공"),
        @ApiResponse(
            responseCode = "404",
            description = "관련 자원(사용자/피드) 미존재",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "피드 좋아요 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<Void> create(
        @PathVariable UUID feedId,
        @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails principal
    );

    @Operation(summary = "피드 좋아요 취소")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "피드 좋아요 취소 성공"),
        @ApiResponse(
            responseCode = "404",
            description = "관련 자원(피드/좋아요) 미존재",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "피드 좋아요 취소 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<Void> delete(
        @PathVariable UUID feedId,
        @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails principal
    );
}
