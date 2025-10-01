package com.samsamotot.otboo.recommendation.controller.api;

import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.recommendation.dto.RecommendationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "추천 관리", description = "추천 관련 API")
public interface RecommendationApi {

    @Operation(summary = "추천 조회")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "추천 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RecommendationDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "관련 자원(날씨) 미존재",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "추천 조회 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<RecommendationDto> getRecommendations(
        @RequestParam UUID weatherId
    );
}
