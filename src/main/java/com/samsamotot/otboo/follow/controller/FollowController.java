package com.samsamotot.otboo.follow.controller;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.follow.controller.api.FollowApi;
import com.samsamotot.otboo.follow.dto.*;
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
public class FollowController implements FollowApi {

    private final FollowService followService;

    /**
     * 새로운 팔로우 관계를 생성한다.
     * 요청으로 전달된 팔로워 ID와 팔로위 ID를 기반으로
     * 팔로우를 생성하고, 생성된 팔로우 정보를 반환한다.
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


    // TODO 팔로우 요약 정보 조회 - 주석 추가
    @GetMapping("/summary")
    public ResponseEntity<FollowSummaryDto> followSummary(@RequestParam UUID userId) {
        return ResponseEntity.ok().body(followService.findFollowSummaries(userId));
    }


    /**
     * 특정 사용자가 팔로우하고 있는 사용자 목록을 조회한다.
     * 요청 파라미터로 전달된 followerId(팔로워 ID)를 기준으로
     * 해당 사용자가 팔로우 중인 사용자 목록을 페이징 처리하여 반환한다.
     * 요청 파라미터는 다음과 같다:
     * <ul>
     *   <li>followerId: 조회 대상 팔로워의 사용자 ID (필수)</li>
     *   <li>cursor: 커서 기반 페이지네이션을 위한 문자열 (선택)</li>
     *   <li>idAfter: 특정 ID 이후의 데이터를 조회하기 위한 UUID (선택)</li>
     *   <li>limit: 조회할 데이터 개수 제한 (필수)</li>
     *   <li>nameLike: 사용자 이름 검색 조건 (부분 일치, 선택)</li>
     * </ul>
     *
     * @param followerId 팔로잉 목록을 조회할 사용자 ID
     * @param cursor     커서 기반 페이지네이션을 위한 커서 값 (nullable)
     * @param idAfter    특정 ID 이후의 데이터를 조회하기 위한 UUID (nullable)
     * @param limit      한 번에 조회할 데이터 개수
     * @param nameLike   사용자 이름 검색 조건 (nullable, 부분 일치)
     * @return 팔로잉 사용자 목록과 페이징 정보를 담은 ResponseEntity (HTTP 200 OK)
     * @throws OtbooException 잘못된 사용자 ID, 유효하지 않은 파라미터 값 등이 전달될 경우
     */
    @GetMapping("/followings")
    public ResponseEntity<FollowListResponse> getFollowings(
        @RequestParam UUID followerId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter, //
        @RequestParam Integer limit,
        @RequestParam(required = false) String nameLike
    ) {
        FollowingRequest request = new FollowingRequest(followerId, cursor, idAfter, limit, nameLike);
        return ResponseEntity.ok().body(followService.getFollowings(request));
    }

    /**
     * 특정 사용자를 팔로우하는 사용자(팔로워) 목록을 조회한다.
     * 요청 파라미터로 전달된 followeeId(대상 사용자 ID)를 기준으로
     * 해당 사용자를 팔로우 중인 사용자 목록을 커서 기반으로 페이징하여 반환한다.
     * 요청 파라미터는 다음과 같다:
     * <ul>
     *   <li>followeeId: 조회 대상(팔로위) 사용자 ID (필수)</li>
     *   <li>cursor: 커서 기반 페이지네이션을 위한 문자열 (선택)</li>
     *   <li>idAfter: 동일 createdAt 시 보조 커서로 사용할 UUID (선택)</li>
     *   <li>limit: 조회할 데이터 개수 제한 (필수)</li>
     *   <li>nameLike: 팔로워 이름 검색 조건 (부분 일치, 선택)</li>
     * </ul>
     *
     * @param followeeId 대상 사용자 ID
     * @param cursor     커서 문자열 (nullable)
     * @param idAfter    보조 커서 UUID (nullable)
     * @param limit      조회 건수
     * @param nameLike   이름 검색(부분 일치, nullable)
     * @return 팔로워 목록과 페이징 정보를 담은 ResponseEntity (HTTP 200 OK)
     * @throws OtbooException 대상 사용자 미존재/잠금 등 서비스 예외 전파
     */
    @GetMapping("/followers")
    public ResponseEntity<FollowListResponse> getFollowers(
        @RequestParam UUID followeeId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam Integer limit,
        @RequestParam(required = false) String nameLike
    ) {
        FollowingRequest request = new FollowingRequest(followeeId, cursor, idAfter, limit, nameLike);
        return ResponseEntity.ok().body(followService.getFollowers(request));
    }


    /**
     * 기존 팔로우 관계를 취소(언팔로우)한다.
     * 전달된 followId를 기준으로 해당 팔로우 엔티티를 삭제한다.
     *
     * @param followId 삭제할 팔로우 관계의 고유 ID
     * @return HTTP 204 No Content 응답 (본문 없음)
     * @throws OtbooException 존재하지 않는 팔로우 ID거나,
     *                        기타 서비스 예외가 발생한 경우
     */
    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> deleteFollow(@PathVariable UUID followId) {
        followService.unfollow(followId);
        return ResponseEntity.noContent().build();
    }
}
