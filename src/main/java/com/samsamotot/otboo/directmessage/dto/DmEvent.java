package com.samsamotot.otboo.directmessage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("message") String content,
    Instant createdAt,
    String status,
    UUID tempId
) {}
