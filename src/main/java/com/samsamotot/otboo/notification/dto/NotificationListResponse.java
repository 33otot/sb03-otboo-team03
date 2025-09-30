package com.samsamotot.otboo.notification.dto;

import com.samsamotot.otboo.directmessage.dto.DirectMessageDto;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.notification.dto
 * FileName     : NotificationListResponse
 * Author       : dounguk
 * Date         : 2025. 9. 30.
 */
@Builder
public record NotificationListResponse(
    List<NotificationDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) { }
