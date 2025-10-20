package com.samsamotot.otboo.common.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import com.samsamotot.otboo.feed.document.FeedDocument;
import com.samsamotot.otboo.feed.repository.FeedRepository;
import com.samsamotot.otboo.feed.repository.FeedSearchRepository;
import com.samsamotot.otboo.feed.service.FeedDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;

@Slf4j
@Profile("!test")
@Configuration
@RequiredArgsConstructor
public class ElasticsearchIndexConfig implements CommandLineRunner {

    private static final String CONFIG = "[ElasticsearchIndexConfig] ";

    private final ElasticsearchOperations operations;
    private final ElasticsearchClient esClient;
    private final FeedDataSyncService feedDataSyncService;
    private final FeedSearchRepository feedSearchRepository;
    private final FeedRepository feedRepository;

    @Value("${spring.elasticsearch.health-timeout-ms:10000}")
    private long healthTimeoutMs;

    @Override
    public void run(String... args) {
        // 인덱스 생성(+ @Setting/@Mapping 적용)
        IndexCreationStatus status = createIndexIfNotExists(FeedDocument.class);
        String indexName = status.indexName();

        // 인덱스 헬스 대기 (최대 10초)
        waitForYellowOrGreen(indexName, healthTimeoutMs);

        // 동기화
        if (status.created()) {
            log.debug(CONFIG + "Elasticsearch data 초기 동기화 시작");
            feedDataSyncService.syncAllFeedsToElasticsearch();
            log.debug(CONFIG + "Elasticsearch data 초기 동기화 완료");
        } else {
            long esCount = feedSearchRepository.count();
            long dbCount = feedRepository.count();
            if (esCount < dbCount) {
                log.warn(CONFIG + "인덱스 문서 수({})가 DB 레코드 수({})보다 적음 → 재동기화 실행", esCount, dbCount);
                feedDataSyncService.syncAllFeedsToElasticsearch();
            } else {
                log.debug(CONFIG + "기존 인덱스이므로 전체 동기화를 스킵합니다.");
            }
        }
    }

    private IndexCreationStatus createIndexIfNotExists(Class<?> documentClass) {
        IndexOperations indexOps = operations.indexOps(documentClass);
        String indexName = indexOps.getIndexCoordinates().getIndexName();

        if (indexOps.exists()) {
            log.debug(CONFIG + "인덱스가 존재하여 생성 스킵함. {}", indexName);
            return new IndexCreationStatus(indexName, false);
        }

        log.debug(CONFIG + "인덱스가 존재하지 않아 생성 시도: {}", indexName);
        try {
            // @Setting/@Mapping 기반 settings & mapping 생성
            Document settings = Document.from(indexOps.createSettings());   // 없으면 빈 Document
            Document mapping  = indexOps.createMapping(documentClass);  // 없으면 엔티티 기반 생성

            // settings 포함 인덱스 생성
            if (settings != null && !settings.isEmpty()) {
                indexOps.create(settings);
                log.debug(CONFIG + "settings 적용됨: {}", settings);
            } else {
                indexOps.create();
                log.debug(CONFIG + "settings 없음, 기본값으로 인덱스 생성됨.");
            }

            // 매핑 적용
            if (mapping != null && !mapping.isEmpty()) {
                indexOps.putMapping(mapping);
                log.debug(CONFIG + "mapping 적용됨: {}", mapping);
            }

            log.debug(CONFIG + "인덱스 생성 완료. {}", indexName);
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("resource_already_exists_exception")) {
                log.warn(CONFIG + "인덱스가 이미 생성되어 있음(경쟁 상황). 계속 진행: {}", indexName);
                return new IndexCreationStatus(indexName, false);
            } else {
                log.error(CONFIG + "인덱스/매핑 생성 실패 → 정리 시도: {}", indexName, e);
                try { if (indexOps.exists()) indexOps.delete(); } catch (Exception ignore) {}
                throw e;
            }
        }
        return new IndexCreationStatus(indexName, true);
    }

    private void waitForYellowOrGreen(String indexName, long timeoutMs) {
        try {
            HealthResponse hr = esClient.cluster().health(h -> h
                .index(indexName)
                .waitForStatus(HealthStatus.Yellow)
                .timeout(t -> t.time(timeoutMs + "ms"))
            );
            log.debug(CONFIG + "인덱스 헬스: {} (index={})", hr.status(), indexName);
        } catch (Exception e) {
            log.warn(CONFIG + "인덱스 헬스 대기 실패/타임아웃: {} (index={})", e.getMessage(), indexName);
        }
    }

    private record IndexCreationStatus(String indexName, boolean created) {}
}