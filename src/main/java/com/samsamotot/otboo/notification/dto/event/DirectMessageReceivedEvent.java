package com.samsamotot.otboo.notification.dto.event;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto.event
 * FileName     : DirectMessageReceivedEvent
 * Author       : dounguk
 * Date         : 2025. 10. 7.
 */
public record DirectMessageReceivedEvent(UUID senderId, UUID receiverId, String content) {
}
