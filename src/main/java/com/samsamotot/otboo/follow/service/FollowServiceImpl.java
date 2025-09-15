package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.dto.FollowSummaryDto;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.follow.mapper.FollowMapper;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private static final String SERVICE = "[FollowService] ";

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;

    /**
     * 새로운 팔로우 관계를 생성한다.
     * <p>
     * 이 메서드는 다음 단계를 수행한다:
     * <ol>
     *   <li>요청으로 전달된 followerId, followeeId에 대한 중복 팔로우 여부를 확인한다.</li>
     *   <li>팔로워(follower)와 팔로위(followee) 사용자가 존재하는지 조회한다.</li>
     *   <li>두 사용자 중 하나라도 잠금 상태(locked)라면 예외를 발생시킨다.</li>
     *   <li>팔로우 엔티티를 생성하고 DB에 저장한다.</li>
     *   <li>저장된 팔로우 엔티티를 {@link FollowDto}로 변환하여 반환한다.</li>
     * </ol>
     *
     * <p><b>예외 처리</b></p>
     * <ul>
     *   <li>{@link OtbooException} - 중복 팔로우 요청, 존재하지 않는 사용자,
     *       혹은 잠금 계정에 대한 요청일 경우 발생</li>
     * </ul>
     *
     * @param request 팔로우 요청 정보 (팔로워 ID, 팔로위 ID)
     * @return 생성된 팔로우 정보를 담은 DTO
     * @throws OtbooException 중복 팔로우, 존재하지 않는 사용자, 잠금 계정인 경우
     */
    @Override
    public FollowDto follow(FollowCreateRequest request) {

        log.info(SERVICE + "팔로우 요청 수신: followerId={}, followeeId={}", request.followerId(), request.followeeId());

        boolean isFollowExists = followRepository.existsFollowByFollowerIdAndFolloweeId(request.followerId(), request.followeeId());

        if (isFollowExists) {
            log.warn(SERVICE + "중복 팔로우 감지: followerId={}, followeeId={}", request.followerId(), request.followeeId());
            throw new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
        }

        User follower = userRepository.findById(request.followerId())
            .orElseThrow(() -> {
                log.warn(SERVICE + "팔로워 사용자 없음: followerId={}", request.followerId());
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
            });

        if (follower.isLocked()) {
            log.warn(SERVICE + "잠금 계정(팔로워): followerId={}", follower.getId());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

        User followee = userRepository.findById(request.followeeId())
            .orElseThrow(() -> {
                log.warn(SERVICE + "팔로우 대상 사용자 없음: followeeId={}", request.followeeId());
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST);
            });

        if (followee.isLocked()) {
            log.warn(SERVICE + "잠금 계정(팔로위): followeeId={}", followee.getId());
            throw new OtbooException(ErrorCode.USER_LOCKED);
        }

        Follow follow = Follow.builder()
            .follower(follower)
            .followee(followee)
            .build();

        Follow savedFollow = followRepository.save(follow);
        log.info(SERVICE + "팔로우 생성 완료: followId={}, followerId={}, followeeId={}", savedFollow.getId(), follower.getId(), followee.getId());

        // TODO 진짜 userSummaryDto 만들어지면 Follow package 내부 UserSummaryDto 삭제, FollowMapper 수정 필요
        return followMapper.toDto(savedFollow);
    }

    // 팔로우 요약 정보 조회
    @Override
    public FollowSummaryDto findFollowSummaries(UUID userId) {
//        UUID followeeId, // 팔로우 대상 사용자 ID - given
//        Long followerCount, // 팔로워 수 - follow
//        Long followingCount, // 팔로잉 수 - follow
//        boolean followedByMe, // 내가 팔로우 대상 사용자를 팔로우 하고 있는지 여부 - follow
//        UUID followedByMeId, // 내가 팔로우 대상 사용자를 팔로우하고 있는 팔로우 ID - follow
//        boolean followingMe // 대상 사용자가 나를 팔로우하고 있는지 여부 - follow


        // findFollowerByFolloweeId
        // findFolloweeByFollowerId
        // existByFollowerAndFollowee 정방향
        // 위에서 true 면 아이디 가져옴
        // existByFollowerAndFollowee 역방향 조회

        /**
         * 1. 로그인 유저 id 가져온다.
         * 2. 대상 사용자 존재 여부 확인
         * 3. locked 여부 확인
         * 4. 대상 사용자 기준 팔로우 수 확인
         * 5. 대상 사용자 기준 팔로잉 수 확인
         * 6. 내가 대상 사용자를 팔로잉 중인지 확인
         * 6-A. 팔로잉 중이면 팔로우 id 확인
         * 7. 대상 사용자가 나를 팔로잉 중인지 확인
         * 8. dto로 매핑 후 return
         */



        return null;
    }
}
