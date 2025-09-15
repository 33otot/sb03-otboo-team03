package com.samsamotot.otboo.feed.service;

import com.samsamotot.otboo.common.dto.CursorResponse;
import com.samsamotot.otboo.feed.dto.FeedCreateRequest;
import com.samsamotot.otboo.feed.dto.FeedCursorRequest;
import com.samsamotot.otboo.feed.dto.FeedDto;
import java.util.UUID;

public interface FeedService {

    FeedDto create(FeedCreateRequest feedCreateRequest);

    CursorResponse<FeedDto> getFeeds(FeedCursorRequest request, UUID userId);
}
