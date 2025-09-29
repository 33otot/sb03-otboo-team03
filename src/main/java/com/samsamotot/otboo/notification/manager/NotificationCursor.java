package com.samsamotot.otboo.notification.manager;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.manager
 * FileName     : NotificationCursor
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */

public final class NotificationCursor {
    private NotificationCursor() {}

    public static String encode(Instant createdAt, UUID id) {
        return createdAt.toEpochMilli() + ":" + id;
    }

    public static Decoded decode(String s) {
        String[] p = s.split(":", 2);
        return new Decoded(Instant.ofEpochMilli(Long.parseLong(p[0])), UUID.fromString(p[1]));
    }

    public record Decoded(Instant ts, UUID id) {}
}
