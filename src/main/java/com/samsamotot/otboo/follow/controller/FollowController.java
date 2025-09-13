package com.samsamotot.otboo.follow.controller;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowListResponse;
import com.samsamotot.otboo.follow.dto.FollowSummaryDto;
import com.samsamotot.otboo.follow.service.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow
 * FileName     : FollowController
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 * Description  : 팔로우 관련 기능을 담당하는 컨트롤러
 */
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("api/follows")
public class FollowController {

    private final FollowService followService;

    /**
     * 새로운 팔로우 관계를 생성한다.
     *
     * 요청으로 전달된 팔로워 ID와 팔로위 ID를 기반으로
     * 팔로우를 생성하고, 생성된 팔로우 정보를 반환한다.
     *
     * 요청 본문은 {@link FollowCreateRequest} 형식이며,
     * 유효성 검증(@Valid)이 수행된다.
     *
     * @param request 팔로우 생성 요청 DTO (followerId, followeeId 포함)
     * @return 생성된 팔로우 정보를 담은 ResponseEntity (HTTP 201 Created)
     * @throws OtbooException 잘못된 요청, 중복 팔로우, 존재하지 않는 사용자,
     *                        잠금 계정에 대한 요청일 경우
     */
    @PostMapping
    public ResponseEntity<FollowDto> follow(@RequestBody @Valid FollowCreateRequest request) {
        FollowDto follow = followService.follow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(follow);
    }

    // 팔로우 요약 정보 조회
//    @GetMapping("/summary")
//    public ResponseEntity<FollowSummaryDto> followSummary(@RequestParam UUID userId) {
//        return ResponseEntity.ok().body(new FollowSummaryDto());
//    }

    // 팔로잉 목록 조회
//    @GetMapping("/followings")
//    public ResponseEntity<FollowListResponse> getFollowings(
//        @RequestParam UUID followerId,
//        @RequestParam(required = false) String cursor,
//        @RequestParam(required = false) String idAfter,
//        @RequestParam Integer limit,
//        @RequestParam(required = false) String nameLike
//    ) {
//        return ResponseEntity.ok().body(new FollowListResponse());
//    }

    // 팔로워 목록 조회
//    @GetMapping("/followers")
//    public ResponseEntity<FollowListResponse> getFollowers(
//        @RequestParam UUID followeeId,
//        @RequestParam(required = false) String cursor,
//        @RequestParam(required = false) String idAfter,
//        @RequestParam Integer limit,
//        @RequestParam(required = false) String nameLike
//    ) {
//        return ResponseEntity.ok().body(new FollowListResponse());
//    }


    // 팔로우 취소
//    @DeleteMapping("/{followId}")
//    public ResponseEntity<Void> deleteFollow(@PathVariable UUID followId) {
//        return ResponseEntity.noContent().build();
//    }
}
