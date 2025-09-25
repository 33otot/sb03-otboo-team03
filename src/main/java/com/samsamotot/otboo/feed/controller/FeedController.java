package com.samsamotot.otboo.feed.controller;

import static com.samsamotot.otboo.common.util.AuthUtil.getAuthenticatedUserId;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.security.service.CustomUserDetails;
import com.samsamotot.otboo.feed.controller.api.FeedApi;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedCursorRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.dto.FeedUpdateRequest;
import com.samsamotot.otboo.feed.service.FeedService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피드(Feed) 관련 API 요청을 처리하는 REST 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class FeedController implements FeedApi {

    private final FeedService feedService;

    /**
     * 새로운 피드를 등록합니다.
     * 피드 생성 로직을 서비스 계층에 위임하고, 생성된 피드 정보를 반환합니다.
     *
     * @param feedCreateRequest 피드 생성 요청 DTO
     * @return 생성된 피드 정보를 담은 ResponseEntity (HTTP 201 Created)
     */
    @Override
    @PostMapping
    public ResponseEntity<FeedDto> createFeed(
        @Valid @RequestBody FeedCreateRequest feedCreateRequest
    ) {
        log.info("[FeedController] 피드 등록 요청: {}", feedCreateRequest);
        FeedDto feedDto = feedService.create(feedCreateRequest);
        log.info("[FeedController] 피드 등록 완료: {}", feedDto);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(feedDto);
    }

    /**
     * 피드 목록을 커서 기반 페이징으로 조회합니다.
     *
     * @param request 커서, 정렬 기준/방향, limit 크기, 검색 조건 등 페이징 요청 DTO
     * @param principal 현재 인증된 사용자 정보
     * @return 조회된 피드 목록, 다음 커서 정보, 전체 개수 등을 포함하는 ResponseEntity (HTTP 200 OK)
     */
    @GetMapping
    @Override
    public ResponseEntity<CursorResponse<FeedDto>> getFeeds(
        @Valid @ModelAttribute FeedCursorRequest request,
        @AuthenticationPrincipal CustomUserDetails principal
    ) {
        log.info("[FeedController] 피드 목록 조회 요청: request = {}", request);

        UUID userId = getAuthenticatedUserId(principal);
        CursorResponse<FeedDto> result = feedService.getFeeds(request, userId);

        log.info("[FeedController] 피드 목록 조회 완료: {}개", result.totalCount());

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }

    /**
     * 특정 피드를 수정합니다.
     *
     * @param feedId 수정할 피드의 ID
     * @param feedUpdateRequest 피드 수정 요청 DTO
     * @param principal 현재 인증된 사용자 정보
     * @return 수정된 피드 정보를 담은 ResponseEntity (HTTP 200 OK)
     */
    @Override
    @PatchMapping("/{feedId}")
    public ResponseEntity<FeedDto> updateFeed(
        @PathVariable UUID feedId,
        @Valid @RequestBody FeedUpdateRequest feedUpdateRequest,
        @AuthenticationPrincipal CustomUserDetails principal
    ) {
        log.info("[FeedController] 피드 수정 요청: {}", feedUpdateRequest);

        UUID userId = getAuthenticatedUserId(principal);
        FeedDto result = feedService.update(feedId, userId, feedUpdateRequest);

        log.info("[FeedController] 피드 수정 완료: {}", result);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }

    /**
     * 특정 피드를 삭제합니다. (논리 삭제)
     *
     * @param feedId 삭제할 피드의 ID
     * @param principal 현재 인증된 사용자 정보
     * @return 내용 없이 HTTP 204 No Content 상태 코드를 담은 ResponseEntity
     */
    @Override
    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
        @PathVariable UUID feedId,
        @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID userId = getAuthenticatedUserId(principal);
        log.info("[FeedController] 피드 삭제 요청: feedId = {}, userId = {}", feedId, userId);

        feedService.delete(feedId, userId);

        log.info("[FeedController] 피드 삭제 완료: feedId = {}, userId = {}", feedId, userId);

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }
}
