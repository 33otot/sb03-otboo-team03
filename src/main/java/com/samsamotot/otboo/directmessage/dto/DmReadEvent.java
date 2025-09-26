package com.samsamotot.otboo.directmessage.dto;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : DmReadEvent
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
public record DmReadEvent(
    UUID peerId,        // 대화 상대 ID
    UUID readerId,      // 읽은 사람(나) ID
    UUID lastMessageId, // 읽음 처리 기준 메시지 ID
    int updatedCount    // DB에서 실제로 isRead=true로 바뀐 개수
) {}
