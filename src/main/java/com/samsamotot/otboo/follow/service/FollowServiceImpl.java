package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.follow.dto.*;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.follow.mapper.FollowMapper;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.follow.service
 * FileName     : FollowServiceImpl
 * Author       : dounguk
 * Date         : 2025. 9. 12.
 * Description  : 팔로우 비즈니스 로직의 구현을 담당한다
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {
    private static final String FOLLOW_SERVICE = "[FollowService] ";

    private static final String SORT_DIRECTION_DESCENDING = "DESCENDING";
    private static final String SORT_BY = "createdAt";

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;
    private final NotificationService notificationService;

    /**
     * 새로운 팔로우 관계를 생성한다.
     *
     * 이 메서드는 다음 단계를 수행한다:
     * 1. followerId와 followeeId를 기반으로 중복 팔로우 여부를 확인한다.
     * 2. 팔로워(follower)와 팔로위(followee) 사용자가 실제로 존재하는지 조회한다.
     * 3. 두 사용자 중 하나라도 잠금 상태(locked)라면 예외를 발생시킨다.
     * 4. 팔로우 엔티티를 생성하여 DB에 저장한다.
     * 5. 저장된 팔로우 엔티티를 DTO로 변환하여 반환한다.
     *
     * 예외 처리:
     * - 중복 팔로우 요청
     * - 존재하지 않는 사용자
     * - 잠금 계정에 대한 요청
     *
     * @param request 팔로우 요청 정보 (팔로워 ID, 팔로위 ID)
     * @return 생성된 팔로우 정보를 담은 DTO
     * @throws OtbooException 위 예외 상황이 발생할 경우
     */
    @Transactional
    @Override
    public FollowDto follow(FollowCreateRequest request) {

        log.info(FOLLOW_SERVICE + "팔로우 요청 수신: followerId={}, followeeId={}", request.followerId(), request.followeeId());

        boolean isFollowExists = followRepository.existsByFollowerIdAndFolloweeId(request.followerId(), request.followeeId());

        if (isFollowExists) {
            log.warn(FOLLOW_SERVICE + "중복 팔로우 감지: followerId={}, followeeId={}", request.followerId(), request.followeeId());
            throw new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
        }

        User follower = userRepository.findById(request.followerId())
            .orElseThrow(() -> {
                log.warn(FOLLOW_SERVICE + "팔로워 사용자 없음: followerId={}", request.followerId());
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
            });

        if (follower.isLocked()) {
            log.warn(FOLLOW_SERVICE + "잠금 계정(팔로워): followerId={}", follower.getId());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

        User followee = userRepository.findById(request.followeeId())
            .orElseThrow(() -> {
                log.warn(FOLLOW_SERVICE + "팔로우 대상 사용자 없음: followeeId={}", request.followeeId());
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
            });

        if (followee.isLocked()) {
            log.warn(FOLLOW_SERVICE + "잠금 계정(팔로위): followeeId={}", followee.getId());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

        Follow follow = Follow.builder()
            .follower(follower)
            .followee(followee)
            .build();

        try {
            Follow savedFollow = followRepository.save(follow);
            log.info(FOLLOW_SERVICE + "팔로우 생성 완료: followId={}, followerId={}, followeeId={}",
                savedFollow.getId(), follower.getId(), followee.getId());

            try {
                notificationService.notifyFollow(follower.getId(), followee.getId());
                log.info(FOLLOW_SERVICE + "팔로우 알림 발행 완료: 팔로우당한 사용자={}", followee.getId());
            } catch (Exception e) {
                log.error(FOLLOW_SERVICE + "팔로우 알림 발행 실패 - 팔로우 당한 사용자: {}, 에러: {}",
                    followee.getId(), e.getMessage(), e);
            }

            return followMapper.toDto(savedFollow);

        } catch (DataIntegrityViolationException e) {
            log.warn(FOLLOW_SERVICE + "동시성 중복 팔로우 감지: followerId={}, followeeId={}",
                follower.getId(), followee.getId());
            throw new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
        }
    }


    @Override
    public FollowSummaryDto findFollowSummaries(UUID userId) {
        log.info(FOLLOW_SERVICE + "팔로우 요약 조회 시작: targetUserId={}", userId);
        UUID loggedInUserId = currentUserId();


        User loggedInUser = userRepository.findById(loggedInUserId).orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));
        if(loggedInUser.isLocked()) {
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }
        User targetUser = userRepository.findById(userId).orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));
        if(targetUser.isLocked()) {
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

        boolean isFollowed  = followRepository.existsByFollowerIdAndFolloweeId(userId, loggedInUserId);

        UUID followedByMeId = followRepository.findByFollowerIdAndFolloweeId(loggedInUserId, userId)
            .map(follow -> follow.getId())
            .orElse(null);

        boolean isFollowing = (followedByMeId != null);

        long followerCount  = followRepository.countByFolloweeId(userId);
        long followingCount = followRepository.countByFollowerId(userId);

        FollowSummaryDto response = FollowSummaryDto.builder()
            .followeeId(targetUser.getId())
            .followerCount(followerCount)
            .followingCount(followingCount)
            .followedByMe(isFollowing)
            .followedByMeId(followedByMeId)
            .followingMe(isFollowed)
            .build();
        log.info(FOLLOW_SERVICE + "팔로우 요약 조회 완료: targetUserId={}, followerCount={}, followingCount={}, followedByMe={}, followingMe={}",
            userId, followerCount, followingCount, isFollowing, isFollowed);

        return response;
    }

    /**
     * 특정 사용자가 팔로우하고 있는 사용자 목록을 조회한다.
     *
     *
     * 1. followerId에 해당하는 사용자를 조회하고 존재하지 않으면 {@link OtbooException}을 발생시킨다.
     * 2. 해당 사용자가 잠금 계정일 경우 예외를 발생시킨다.
     * 3. QueryDSL 기반으로 팔로잉 목록을 조회하고, limit + 1 방식으로 다음 페이지 여부(hasNext)를 판별한다.
     * 4. 조회 결과를 기반으로 totalCount, nextCursor, nextIdAfter 등을 계산하여 응답 객체를 구성한다.
     * 5. DTO 매핑을 통해 클라이언트에 반환할 데이터를 생성한다.
     *
     * @param request 팔로잉 목록 조회 요청 DTO
     *                (followerId, cursor, idAfter, limit, nameLike 포함)
     * @return 팔로잉 목록과 페이징 정보를 담은 {@link FollowListResponse}
     * @throws OtbooException 존재하지 않는 사용자이거나, 잠금 계정일 경우 발생
     */
    @Override
    public FollowListResponse getFollowings(FollowingRequest request) {
        log.info(FOLLOW_SERVICE + "팔로잉 목록 조회 시작: followerId={}, limit={}, cursor={}, idAfter={}, nameLike={}",
            request.followerId(), request.limit(), request.cursor(), request.idAfter(), request.nameLike());

        /* 0. cursor 유효성 확인 */
        if (request.cursor() != null && parseCursorToInstant(request.cursor()) == null) {
            log.warn(FOLLOW_SERVICE + "유효하지 않은 커서 타입: cursor={}", request.cursor());
        }

        /* 1. user 검색*/
        User user = userRepository.findById(request.followerId()).orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));

        /* 2. locked 여부 확인*/
        if (user.isLocked()) {
            log.warn(FOLLOW_SERVICE + "잠금 계정(팔로워): followerId={}", user.getId());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

         /* 3. FollowListResponse 변수 가공
            3-1쿼리 dsl 사용 Follow 리스트 */
        List<Follow> follows = followRepository.findFollowings(request);

        /* 3-2: limit*/
        int limit = Math.max(1, request.limit());

        /* 3-3: hasNext*/
        boolean hasNext = follows.size() > limit;

        /* 3-4. totalCount*/
        long totalCount = followRepository.countTotalFollowings(request.followerId(), request.nameLike());

        /*3-5 nextCursor n nextIdAfter*/
        List<Follow> pageRows = hasNext ? follows.subList(0, limit) : follows;
        Instant nextCreatedAt = pageRows.isEmpty() ? null : pageRows.get(pageRows.size() - 1).getCreatedAt();
        String nextCursor = (hasNext && nextCreatedAt != null) ? nextCreatedAt.toString() : null; // nextCursor
        UUID nextIdAfter = pageRows.isEmpty() ? null : pageRows.get(pageRows.size() - 1).getId(); // nextIdAfter

        /* 3-6: data*/
        List<FollowDto> data = pageRows.stream().map(followMapper::toDto).toList();

        /* 3-7: 페이지에 맞게 null*/
        if (!hasNext) {
            nextIdAfter = null;
        }

        FollowListResponse response = FollowListResponse.builder()
            .data(data)
            .nextCursor(nextCursor)
            .nextIdAfter(nextIdAfter)
            .hasNext(hasNext)
            .totalCount(totalCount)
            .sortBy(SORT_BY)
            .sortDirection(SORT_DIRECTION_DESCENDING)
            .build();

        log.info(FOLLOW_SERVICE + "팔로잉 목록 조회 완료: followerId={}, 반환 데이터 개수={}, hasNext={}",
            request.followerId(), data.size(), hasNext);

        return response;
    }


    /**
     * 특정 사용자를 팔로우하는 사용자(팔로워) 목록을 조회한다.
     *
     * 1. 커서 문자열 유효성 검사(형식 불일치 시 경고 로그만 출력)
     * 2. 대상 사용자(=followee) 존재/잠금 여부 확인
     * 3. QueryDSL로 (limit+1)개 행 조회하여 hasNext 계산
     * 4. totalCount, nextCursor(createdAt), nextIdAfter(UUID) 계산
     * 5. DTO 매핑 및 응답 구성
     *
     * @param request 팔로워 목록 조회 요청 DTO
     *                (followeeId → {@code request.followerId()}에 전달, cursor, idAfter, limit, nameLike 포함)
     * @return 팔로워 목록과 페이징 정보를 담은 {@link FollowListResponse}
     * @throws OtbooException 존재하지 않는 사용자이거나, 잠금 계정일 경우 발생
     */
    @Override
    public FollowListResponse getFollowers(FollowingRequest request) {
        log.info(FOLLOW_SERVICE + "팔로워 목록 조회 시작: followeeId(target)={}, limit={}, cursor={}, idAfter={}, nameLike={}",
            request.followerId(), request.limit(), request.cursor(), request.idAfter(), request.nameLike());

        /* 0. cursor 유효성 확인 */
        if (request.cursor() != null && parseCursorToInstant(request.cursor()) == null) {
            log.warn(FOLLOW_SERVICE + "유효하지 않은 커서 타입: cursor={}", request.cursor());
        }

        /* 1. 대상 사용자(=followee) 조회 및 잠금 체크 */
        User user = userRepository.findById(request.followerId())
            .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND));

        if (user.isLocked()) {
            log.warn(FOLLOW_SERVICE + "잠금 계정(팔로위/대상): followeeId={}", user.getId());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

        /* 2. 목록 조회 (limit+1) */
        List<Follow> follows = followRepository.findFollowers(request);

        /* 3. 페이징 계산 */
        int limit = Math.max(1, request.limit());
        boolean hasNext = follows.size() > limit;

        long totalCount = followRepository.countTotalFollowers(request.followerId(), request.nameLike());

        List<Follow> pageRows = hasNext ? follows.subList(0, limit) : follows;

        Instant nextCreatedAt = pageRows.isEmpty() ? null : pageRows.get(pageRows.size() - 1).getCreatedAt();
        String nextCursor = (hasNext && nextCreatedAt != null) ? nextCreatedAt.toString() : null;

        UUID nextIdAfter = pageRows.isEmpty() ? null : pageRows.get(pageRows.size() - 1).getId();
        if (!hasNext) {
            nextIdAfter = null;
        }

        /* 4. DTO 매핑 */
        List<FollowDto> data = pageRows.stream().map(followMapper::toDto).toList();

        FollowListResponse response = FollowListResponse.builder()
            .data(data)
            .nextCursor(nextCursor)
            .nextIdAfter(nextIdAfter)
            .hasNext(hasNext)
            .totalCount(totalCount)
            .sortBy(SORT_BY)
            .sortDirection(SORT_DIRECTION_DESCENDING)
            .build();

        log.info(FOLLOW_SERVICE + "팔로워 목록 조회 완료: followeeId={}, 반환 데이터 개수={}, hasNext={}",
            request.followerId(), data.size(), hasNext);

        return response;
    }

    /**
     * 기존 팔로우 관계를 취소(언팔로우)한다.
     *
     * 1. 전달된 followId(팔로우 관계 고유 ID)를 기준으로 존재 여부 확인
     *    - 존재하지 않으면 {@link OtbooException} 발생 (ErrorCode.FOLLOW_NOT_FOUND)
     * 2. 존재할 경우 해당 팔로우 관계를 삭제
     * 3. 시작/완료 로그 기록
     *
     * @param followId 언팔로우할 팔로우 관계의 고유 ID
     * @throws OtbooException followId가 존재하지 않을 경우 발생
     */
    @Transactional
    @Override
    public void unfollow(UUID followId) {
        log.info(FOLLOW_SERVICE + "언팔로우 시작: followeeId={}", followId);
        if (!followRepository.existsById(followId)) {
            throw new OtbooException(ErrorCode.FOLLOW_NOT_FOUND);
        }
        followRepository.deleteById(followId);
        log.info(FOLLOW_SERVICE + "언팔로우 완료");
    }

    private static Instant parseCursorToInstant(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(cursor);
        } catch (Exception ignore) {
        }
        try {
            return java.time.OffsetDateTime.parse(cursor).toInstant();
        } catch (Exception ignore) {
        }
        try {
            return java.time.LocalDateTime.parse(cursor).toInstant(java.time.ZoneOffset.UTC);
        } catch (Exception ignore) {
        }
        return null;
    }

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            log.warn(FOLLOW_SERVICE + "인증 실패: Authentication 비어있음/미인증 (auth={}, principal={})", auth, (auth != null ? auth.getPrincipal() : null));
            throw new OtbooException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
            .map(User::getId)
            .orElseThrow(() -> {
                log.warn(FOLLOW_SERVICE + "인증 사용자 조회 실패: email={} (DB에 없음)", email);
                return new OtbooException(ErrorCode.UNAUTHORIZED);
            });
    }
}
