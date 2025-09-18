package com.samsamotot.otboo.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record FeedUpdateRequest(

    @NotBlank(message = "내용은 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "내용은 최대 1000자까지 가능합니다.")
    String content
) {

}
