package com.samsamotot.otboo.feed.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FeedCreateRequest(
    UUID authorId,
    UUID weatherId,
    List<UUID> clothesIds,
    String content
) {

}
