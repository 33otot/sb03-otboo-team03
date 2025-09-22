package com.samsamotot.otboo.follow.controller.api;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.exception.ErrorResponse;
import com.samsamotot.otboo.feed.dto.FeedCursorRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowListResponse;
import com.samsamotot.otboo.follow.dto.FollowSummaryDto;
import com.samsamotot.otboo.follow.entity.Follow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.controller.api
 * FileName     : FollowApi
 * Author       : dounguk
 * Date         : 2025. 9. 22.
 */
@Tag(name = "팔로우 관리", description = "팔로우 관련 API")
public interface FollowApi {

    @Operation(summary = "팔로우 등록")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "팔로우 등록 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FollowDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "팔로우 생성 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<FollowDto> follow(
        @RequestBody @Valid FollowCreateRequest request
    );

    @Operation(summary = "팔로우 요약 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "팔로우 요약 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FollowSummaryDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "팔로우 조회 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<FollowSummaryDto> followSummary(
        @RequestParam UUID userId
    );

    @Operation(summary = "팔로잉 목록 조회")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "팔로잉 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FollowListResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "팔로잉 목록 조회 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/followings")
    ResponseEntity<FollowListResponse> getFollowings(
        @RequestParam UUID followerId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit,
        @RequestParam(required = false) String nameLike
    );


    @Operation(summary = "팔로워 목록 조회")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "팔로워 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FollowListResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "팔로워 목록 조회 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    ResponseEntity<FollowListResponse> getFollowers(
        @RequestParam UUID followeeId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit,
        @RequestParam(required = false) String nameLike
    );

    @Operation(summary = "팔로우 취소")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "팔로우 취소 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "팔로우 취소 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @DeleteMapping("/{followId}")
    ResponseEntity<Void> deleteFollow(@PathVariable UUID followId);

}




