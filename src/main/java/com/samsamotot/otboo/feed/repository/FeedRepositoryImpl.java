package com.samsamotot.otboo.feed.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.QFeed;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private final QFeed qFeed = QFeed.feed;

    @Override
    public List<Feed> findByCursor(String cursor, UUID idAfter, int limit, String sortBy,
        SortDirection sortDirection, String keywordLike, SkyStatus skyStatusEqual,
        Precipitation precipitationTypeEqual, UUID authorIdEqual) {
        return List.of();
    }

    @Override
    public long countByFilter(String keywordLike, SkyStatus skyStatusEqual,
        Precipitation precipitationTypeEqual, UUID authorIdEqual) {
        return 0;
    }
}
