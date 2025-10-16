package com.samsamotot.otboo.feed.repository;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.common.type.SortDirection;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.weather.entity.Precipitation;
import com.samsamotot.otboo.weather.entity.SkyStatus;
import java.util.UUID;

public interface FeedSearchRepositoryCustom {

    CursorResponse<FeedDto> findByCursor(
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
}
