package com.samsamotot.otboo.feed.service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedLike;
import com.samsamotot.otboo.feed.repository.FeedLikeRepository;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.notification.service.NotificationService;
import com.samsamotot.otboo.user.entity.User;
import com.samsamotot.otboo.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드 좋아요(FeedLike) 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeedLikeServiceImpl implements FeedLikeService {

    private static final String SERVICE = "[FeedLikeServiceImpl] ";

    private final FeedLikeRepository feedLikeRepository;
    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 피드에 좋아요를 생성합니다.
     *
     * @param feedId 좋아요를 추가할 피드의 ID
     * @param userId 좋아요를 누른 사용자의 ID
     * @return 생성된 피드 좋아요(FeedLike) 객체
     * @throws OtbooException 사용자/피드를 찾을 수 없는 경우
     *                        이미 좋아요를 누른 피드인 경우
     */
    @Override
    public FeedLike create(UUID feedId, UUID userId) {

        log.debug(SERVICE + "피드 좋아요 생성 시작: feedId = {}, userId = {}", feedId, userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new OtbooException(ErrorCode.USER_NOT_FOUND, Map.of("userId", userId.toString())));

        Feed feed = feedRepository.findByIdAndIsDeletedFalse(feedId)
            .orElseThrow(() -> new OtbooException(ErrorCode.FEED_NOT_FOUND, Map.of("feedId", feedId.toString())));

        if (feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)) {
            throw new OtbooException(ErrorCode.FEED_ALREADY_LIKED);
        }

        FeedLike feedLike = FeedLike.builder()
            .user(user)
            .feed(feed)
            .build();
        try {
            feedLikeRepository.save(feedLike);
            feedRepository.incrementLikeCount(feedId); // 좋아요 수 증가
        } catch (DataIntegrityViolationException e) {
            // 유니크 제약 위반 등 동시성 충돌 → 멱등하게 409로 매핑
            throw new OtbooException(ErrorCode.FEED_ALREADY_LIKED);
        }

        log.debug(SERVICE + "피드 좋아요 생성 완료 feedLikeId = {}", feedLike.getId());

        notificationService.notifyLike(userId, feedId);

        log.debug(SERVICE + "좋아요 알림 전송 완료 feedLikeId = {}", feedLike.getId());

        return feedLike;
    }


    /**
     * 피드에 추가된 좋아요를 취소합니다.
     *
     * @param feedId 좋아요를 취소할 피드의 ID
     * @param userId 좋아요를 취소하는 사용자의 ID
     * @throws OtbooException 피드 또는 좋아요 정보를 찾을 수 없는 경우
     */
    @Override
    public void delete(UUID feedId, UUID userId) {

        log.debug(SERVICE + "피드 좋아요 취소 시작: feedId = {}, userId = {}", feedId, userId);

        Feed feed = feedRepository.findByIdAndIsDeletedFalse(feedId)
            .orElseThrow(() -> new OtbooException(ErrorCode.FEED_NOT_FOUND, Map.of("feedId", feedId.toString())));

        int deletedCnt = feedLikeRepository.deleteByFeedIdAndUserId(feedId, userId);

        if (deletedCnt == 0) {
            throw new OtbooException(ErrorCode.FEED_LIKE_NOT_FOUND);
        }

        feedRepository.decrementLikeCount(feedId); // 좋아요 수 감소

        log.debug(SERVICE + "피드 좋아요 취소 완료 feedId = {}, userId = {}", feedId, userId);
    }
}
