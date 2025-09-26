package com.samsamotot.otboo.directmessage.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : DmEvent
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
@Builder
public record DmEvent(
    UUID id,
    UUID senderId,
    UUID receiverId,
    String content,
    Instant createdAt,
    String status, // "SENT" | "DELIVERED" | "READ"
    UUID tempId    // 요청 tempId를 에코해 클라가 매칭
) {}
