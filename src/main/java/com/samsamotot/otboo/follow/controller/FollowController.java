package com.samsamotot.otboo.follow.controller;

import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowListResponse;
import com.samsamotot.otboo.follow.dto.FollowSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

    // 팔로우 생성
    @PostMapping
    public ResponseEntity<FollowDto> follow(@RequestBody FollowCreateRequest request) {
        return ResponseEntity.ok().body(new FollowDto());
    }

    // 팔로우 요약 정보 조회
    @GetMapping("/summary")
    public ResponseEntity<FollowSummaryDto> followSummary(@RequestParam UUID userId) {
        return ResponseEntity.ok().body(new FollowSummaryDto());
    }

    // 팔로잉 목록 조회
    @GetMapping("/followings")
    public ResponseEntity<FollowListResponse> getFollowings(
        @RequestParam UUID followerId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) String idAfter,
        @RequestParam Integer limit,
        @RequestParam(required = false) String nameLike
    ) {
        return ResponseEntity.ok().body(new FollowListResponse());
    }

    // 팔로워 목록 조회
    @GetMapping("/followers")
    public ResponseEntity<FollowListResponse> getFollowers(
        @RequestParam UUID followeeId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) String idAfter,
        @RequestParam Integer limit,
        @RequestParam(required = false) String nameLike
    ) {
        return ResponseEntity.ok().body(new FollowListResponse());
    }


    // 팔로우 취소
    @DeleteMapping("/{followId")
    public ResponseEntity<Void> deleteFollow(@PathVariable UUID followId) {
        return ResponseEntity.noContent().build();
    }
}
