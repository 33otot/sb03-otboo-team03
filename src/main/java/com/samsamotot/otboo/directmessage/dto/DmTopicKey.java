package com.samsamotot.otboo.directmessage.dto;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : DmTopicKey
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
public final class DmTopicKey {
    private DmTopicKey() {}
    public static String pairId(UUID a, UUID b) {
        String s1 = a.toString(), s2 = b.toString();
        return (s1.compareTo(s2) <= 0) ? (s1 + "-" + s2) : (s2 + "-" + s1);
    }
    public static String topic(UUID a, UUID b) {
        return "/sub/direct-messages_" + pairId(a, b);
    }
}
