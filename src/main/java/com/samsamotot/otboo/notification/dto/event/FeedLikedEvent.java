package com.samsamotot.otboo.notification.dto.event;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto.event
 * FileName     : FeedLikedEvent
 * Author       : dounguk
 * Date         : 2025. 10. 7.
 */
public record FeedLikedEvent(UUID likerId, UUID feedId) {
}
