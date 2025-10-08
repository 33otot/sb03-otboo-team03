package com.samsamotot.otboo.notification.dto.event;

import com.samsamotot.otboo.feed.entity.Feed;
import com.samsamotot.otboo.user.entity.User;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto.event
 * FileName     : FeedCreatedEvent
 * Author       : dounguk
 * Date         : 2025. 10. 8.
 */
public record FeedCreatedEvent(User author) {}
