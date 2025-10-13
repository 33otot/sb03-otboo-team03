package com.samsamotot.otboo.feed.service;

import com.samsamotot.otboo.feed.document.FeedDocument;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.mapper.FeedMapper;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.feed.repository.FeedSearchRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedDataSyncService {

    private final String SERVICE = "[FeedDataSyncService] ";

    private final FeedRepository feedRepository;
    private final FeedSearchRepository feedSearchRepository;
    private final FeedMapper feedMapper;

    @Transactional(readOnly = true)
    public void syncAllFeedsToElasticsearch() {

        log.debug(SERVICE + "Elasticsearch 동기화 시작");
        List<FeedDto> allFeedDtos = feedRepository.findAll().stream()
                .map(feedMapper::toDto)
                .toList();
        List<FeedDocument> feedDocuments = allFeedDtos.stream()
                .map(this::convertToFeedDocument)
                .collect(Collectors.toList());

        if (!feedDocuments.isEmpty()) {
            feedSearchRepository.saveAll(feedDocuments);
            log.debug(SERVICE + "Elasticsearch 동기화 완료 {}건", feedDocuments.size());
        } else {
            log.debug(SERVICE + "Elasticsearch 동기화할 피드가 없습니다.");
        }
    }

    @Transactional
    public void syncFeedToElasticsearch(FeedDto feedDto) {
        FeedDocument feedDocument = convertToFeedDocument(feedDto);
        feedSearchRepository.save(feedDocument);
        log.debug(SERVICE + "Elasticsearch 동기화: {}", feedDto.id());
    }

    @Transactional
    public void deleteFeedFromElasticsearch(UUID feedId) {
        feedSearchRepository.deleteById(feedId);
        log.debug(SERVICE + "Elasticsearch에서 피드 삭제: {}", feedId);
    }

    @Transactional
    public void softDeleteFeedFromElasticsearch(UUID feedId) {
        feedSearchRepository.findById(feedId).ifPresent(feedDocument -> {
            FeedDocument updatedDocument = FeedDocument.builder()
                .id(feedDocument.id())
                .author(feedDocument.author())
                .weather(feedDocument.weather())
                .content(feedDocument.content())
                .createdAt(feedDocument.createdAt())
                .updatedAt(feedDocument.updatedAt())
                .ootds(feedDocument.ootds())
                .likeCount(feedDocument.likeCount())
                .commentCount(feedDocument.commentCount())
                .likedByMe(feedDocument.likedByMe())
                .isDeleted(true) // 소프트 삭제
                .build();
            feedSearchRepository.save(updatedDocument);
            log.debug(SERVICE + "Elasticsearch에서 피드 소프트 삭제: {}", feedId);
        });
    }

    private FeedDocument convertToFeedDocument(FeedDto feedDto) {
        return FeedDocument.builder()
            .id(feedDto.id())
            .author(feedDto.author())
            .weather(feedDto.weather())
            .content(feedDto.content())
            .createdAt(feedDto.createdAt())
            .updatedAt(feedDto.updatedAt())
            .ootds(feedDto.ootds())
            .likeCount(feedDto.likeCount())
            .commentCount(feedDto.commentCount())
            .likedByMe(feedDto.likedByMe())
            .isDeleted(false)
            .build();
    }
}
