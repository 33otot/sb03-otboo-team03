package com.samsamotot.otboo.common.fixture;

import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.feed.entity.FeedLike;
import com.samsamotot.otboo.user.entity.User;

public class FeedLikeFixture {

    public static FeedLike createFeedLike(User user, Feed feed) {
        return FeedLike.builder()
            .feed(feed)
            .user(user)
            .build();
    }
}
