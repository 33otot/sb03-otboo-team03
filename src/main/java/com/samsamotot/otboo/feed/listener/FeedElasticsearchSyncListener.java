package com.samsamotot.otboo.feed.listener;

import com.samsamotot.otboo.feed.dto.event.FeedDeleteEvent;
import com.samsamotot.otboo.feed.dto.event.FeedSyncEvent;
import com.samsamotot.otboo.feed.service.FeedDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedElasticsearchSyncListener {

    private static final String LISTENER = "[FeedElasticsearchSyncListener] ";

    private final FeedDataSyncService feedDataSyncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedSyncEvent(FeedSyncEvent event) {
        log.debug(LISTENER + "피드 Elasticsearch 동기화 이벤트 시작");
        feedDataSyncService.syncFeedToElasticsearch(event.feedDto());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedDeleteEvent(FeedDeleteEvent event) {
        log.debug(LISTENER + "피드 Elasticsearch 논리삭제 이벤트 시작");
        feedDataSyncService.softDeleteFeedFromElasticsearch(event.feedId());
    }
}
