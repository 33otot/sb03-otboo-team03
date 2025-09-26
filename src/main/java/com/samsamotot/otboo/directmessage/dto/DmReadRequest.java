package com.samsamotot.otboo.directmessage.dto;

import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : DmReadRequest
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
public record DmReadRequest(
    UUID peerId,        // 대화 상대 ID
    UUID lastMessageId  // 이 ID 이하 메시지를 읽음 처리
) {}