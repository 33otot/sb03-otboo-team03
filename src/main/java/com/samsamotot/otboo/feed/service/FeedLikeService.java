package com.samsamotot.otboo.feed.service;

import com.samsamotot.otboo.feed.entity.FeedLike;

import java.util.UUID;

public interface FeedLikeService {

    FeedLike create(UUID feedId, UUID userId);

    void delete(UUID feedId, UUID userId);
}
