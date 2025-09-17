package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import java.util.List;
import java.util.UUID;

public interface FeedRepositoryCustom {

    List<Feed> findByCursor(
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        SortDirection sortDirection,
        String keywordLike,
        SkyStatus skyStatusEqual,
        Precipitation precipitationTypeEqual,
        UUID authorIdEqual
    );

    long countByFilter(
        String keywordLike,
        SkyStatus skyStatusEqual,
        Precipitation precipitationTypeEqual,
        UUID authorIdEqual
    );
}
