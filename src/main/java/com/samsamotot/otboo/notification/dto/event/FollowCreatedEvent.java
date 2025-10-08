package com.samsamotot.otboo.notification.dto.event;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto.event
 * FileName     : FollowCreatedEvent
 * Author       : dounguk
 * Date         : 2025. 10. 7.
 */
public record FollowCreatedEvent(UUID followeeId, String followerName) {
}
