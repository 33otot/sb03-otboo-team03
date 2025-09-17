package com.samsamotot.otboo.feed.service;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedCursorRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import com.samsamotot.otboo.feed.dto.FeedUpdateRequest;
import com.samsamotot.otboo.feed.entity.Feed;
import java.util.UUID;

public interface FeedService {

    FeedDto create(FeedCreateRequest feedCreateRequest);

    CursorResponse<FeedDto> getFeeds(FeedCursorRequest request, UUID userId);

    FeedDto update(UUID feedId, UUID userId, FeedUpdateRequest request);

    Feed delete(UUID feedId, UUID userId);
}
