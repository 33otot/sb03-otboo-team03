package com.samsamotot.otboo.comment.controller.api;

import com.samsamotot.otboo.comment.dto.CommentCreateRequest;
import com.samsamotot.otboo.comment.dto.CommentCursorRequest;
import com.samsamotot.otboo.comment.dto.CommentDto;
import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "피드", description = "피드 관련 API")
public interface CommentApi {

    @Operation(summary = "피드 댓글 등록")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "피드 댓글 등록 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommentDto.class)
            )
        ),
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
            description = "피드 댓글 등록 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<CommentDto> createComment(
        @PathVariable("feedId") UUID feedId,
        @Valid @RequestBody CommentCreateRequest commentCreateRequest
    );

    @Operation(summary = "피드 댓글 목록 조회")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "피드 댓글 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CursorResponse.class)
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
            responseCode = "400",
            description = "피드 댓글 목록 조회 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<CursorResponse<CommentDto>> getComments(
        @PathVariable("feedId") UUID feedId,
        @Valid @ParameterObject @ModelAttribute CommentCursorRequest commentCursorRequest
    );
}
