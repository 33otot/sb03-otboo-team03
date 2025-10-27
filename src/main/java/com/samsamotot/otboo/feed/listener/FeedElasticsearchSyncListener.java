package com.samsamotot.otboo.feed.listener;

import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.dto.event.FeedDeleteEvent;
import com.samsamotot.otboo.feed.dto.event.FeedSyncEvent;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.mapper.FeedMapper;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.feed.service.FeedDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
//@Component
@RequiredArgsConstructor
public class FeedElasticsearchSyncListener {

    private static final String LISTENER = "[FeedElasticsearchSyncListener] ";

    private final FeedDataSyncService feedDataSyncService;
    private final FeedRepository feedRepository;
    private final FeedMapper feedMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedSyncEvent(FeedSyncEvent event) {
        try {
            log.debug(LISTENER + "피드 Elasticsearch 동기화 이벤트 시작");
            Feed feed = feedRepository.findById(event.feedId())
                .orElseThrow(() -> new IllegalStateException("Feed not found for sync: " + event.feedId()));
            FeedDto feedDto = feedMapper.toDto(feed);
            feedDataSyncService.syncFeedToElasticsearch(feedDto);
        } catch (Exception e) {
            log.warn(LISTENER + "동기화 실패: feedId=" + event.feedId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedDeleteEvent(FeedDeleteEvent event) {
        log.debug(LISTENER + "피드 Elasticsearch 논리삭제 이벤트 시작");
        feedDataSyncService.softDeleteFeedFromElasticsearch(event.feedId());
    }
}
