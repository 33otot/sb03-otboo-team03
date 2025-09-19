package com.samsamotot.otboo.feed.controller;

import com.samsamotot.otboo.feed.controller.api.FeedLikeApi;
import com.samsamotot.otboo.feed.entity.FeedLike;
import com.samsamotot.otboo.feed.service.FeedLikeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 피드 좋아요(FeedLike) 관련 API 요청을 처리하는 REST 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds/{feedId}/like")
public class FeedLikeController implements FeedLikeApi {

    private final String CONTROLLER = "[FeedLikeController] ";
    private final FeedLikeService feedLikeService;

    /**
     * 피드에 좋아요를 추가합니다.
     *
     * @param feedId 좋아요를 추가할 피드의 ID
     * @param userId 좋아요를 누른 사용자의 ID
     * @return 204 No Content
     */
    @Override
    @PostMapping
    public ResponseEntity<Void> create(
        @PathVariable UUID feedId,
        @RequestParam UUID userId
    ) {
        // TODO : Security 구현 후 @RequestParam userId 제거하고 인증 컨텍스트에서 조회하도록 변경
        //  ex) @AuthenticationPrincipal CustomUserPrincipal principal → UUID userId = principal.getId()
        //  컨트롤러 테스트도 함께 수정 필요

        log.info(CONTROLLER + "피드 좋아요 요청 - feedId = {}, userId = {}", feedId, userId);
        FeedLike result = feedLikeService.create(feedId, userId);
        log.info(CONTROLLER + "피드 좋아요 완료 - feedLikeId = {}", result.getId());

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }
}
