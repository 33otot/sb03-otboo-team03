package com.samsamotot.otboo.feed.controller.api;

import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "피드 관리", description = "피드 관련 API")
public interface FeedApi {

    @Operation(summary = "피드 등록")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "피드 등록 성공",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = FeedDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "피드 등록 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<FeedDto> createFeed(
        FeedCreateRequest feedCreateRequest
    );
}
