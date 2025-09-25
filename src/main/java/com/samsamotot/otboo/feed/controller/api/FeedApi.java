package com.samsamotot.otboo.feed.controller.api;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedCursorRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.dto.FeedUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "피드 관리", description = "피드 관련 API")
public interface FeedApi {

    @Operation(summary = "피드 등록")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "피드 등록 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FeedDto.class)
            )
        ),
        @ApiResponse(
          responseCode = "404",
          description = "관련 자원(사용자/의상/날씨) 미존재",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "피드 등록 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<FeedDto> createFeed(
        @Valid @RequestBody FeedCreateRequest feedCreateRequest
    );

    @Operation(summary = "피드 목록 조회")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "피드 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CursorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "피드 목록 조회 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<CursorResponse<FeedDto>> getFeeds(
        @Valid @ModelAttribute FeedCursorRequest feedCursorRequest
    );

    @Operation(summary = "피드 수정")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "피드 수정 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FeedDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "피드 미존재",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "작성자가 아님",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "피드 수정 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<FeedDto> updateFeed(
        @PathVariable UUID feedId,
        @Valid @RequestBody FeedUpdateRequest feedUpdateRequest
    );

    @Operation(summary = "피드 삭제")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "피드 삭제 성공"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "피드 미존재",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "작성자 아님",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "피드 삭제 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<Void> deleteFeed(
        @PathVariable UUID feedId
    );
}
