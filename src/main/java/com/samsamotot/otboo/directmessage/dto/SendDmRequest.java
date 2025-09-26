package com.samsamotot.otboo.directmessage.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : SendDmRequest
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
public record SendDmRequest(
    UUID toUserId,
    String content,
    Instant clientSentAt, // optional: 클라 타임스탬프
    UUID tempId          // optional: 낙관적 UI 매칭용
) {}
