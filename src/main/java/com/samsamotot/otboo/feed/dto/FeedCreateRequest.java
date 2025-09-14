package com.samsamotot.otboo.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FeedCreateRequest(

    @NotNull
    UUID authorId,

    @NotNull
    UUID weatherId,

    @NotEmpty
    List<UUID> clothesIds,

    @NotBlank
    @Size(max = 1000)
    String content
) {

}
