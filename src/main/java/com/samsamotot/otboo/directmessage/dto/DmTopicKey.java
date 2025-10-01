package com.samsamotot.otboo.directmessage.dto;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : DmTopicKey
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
public final class DmTopicKey {
    private static final String SUB_PREFIX = "/sub";
    private static final String CHANNEL_PREFIX = "direct-messages_";
    private DmTopicKey() {}

    public static String key(UUID a, UUID b) {
        return a.toString().compareTo(b.toString()) <= 0 ? a + "_" + b : b + "_" + a;
    }
    public static String destination(UUID a, UUID b) {
        return SUB_PREFIX + "/" + CHANNEL_PREFIX + key(a, b);
    }
}