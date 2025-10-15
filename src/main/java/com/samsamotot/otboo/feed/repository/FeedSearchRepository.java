package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.feed.document.FeedDocument;
import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedSearchRepository extends ElasticsearchRepository<FeedDocument, UUID>, FeedSearchRepositoryCustom {

}
