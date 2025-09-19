package com.samsamotot.otboo.feed.service;

import com.samsamotot.otboo.feed.entity.FeedLike;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public FeedLike create(UUID feedId, UUID userId) {
        return null;
    }
}
