package com.samsamotot.otboo.directmessage.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

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
    @JsonAlias({"message"}) String content,
    UUID tempId
) {}
