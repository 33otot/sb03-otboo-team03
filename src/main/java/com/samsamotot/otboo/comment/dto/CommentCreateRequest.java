package com.samsamotot.otboo.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CommentCreateRequest(
    @NotNull
    UUID feedId,
    @NotNull
    UUID authorId,
    @NotBlank(message = "내용은 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "내용은 최대 1000자까지 가능합니다.")
    String content
) {

}
