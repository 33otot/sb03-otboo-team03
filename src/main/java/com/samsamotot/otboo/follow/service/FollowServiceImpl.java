package com.samsamotot.otboo.follow.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.follow.dto.FollowCreateRequest;
import com.samsamotot.otboo.follow.dto.FollowDto;
import com.samsamotot.otboo.follow.entity.Follow;
import com.samsamotot.otboo.follow.mapper.FollowMapper;
import com.samsamotot.otboo.follow.repository.FollowRepository;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    // 팔로우 기능
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
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST); // (권장: FOLLOWER_NOT_FOUND)
            });

        if (follower.isLocked()) {
            log.warn(SERVICE + "잠금 계정(팔로워): followerId={}", follower.getId());
            throw new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST); // (권장: USER_LOCKED)
        }

        User followee = userRepository.findById(request.followeeId())
            .orElseThrow(() -> {
                log.warn(SERVICE + "팔로우 대상 사용자 없음: followeeId={}", request.followeeId());
                return new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST); // (권장: FOLLOWEE_NOT_FOUND)
            });

        if (followee.isLocked()) {
            log.warn(SERVICE + "잠금 계정(팔로위): followeeId={}", followee.getId());
            throw new OtbooException(ErrorCode.INVALID_FOLLOW_REQUEST); // (권장: USER_LOCKED)
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
}
