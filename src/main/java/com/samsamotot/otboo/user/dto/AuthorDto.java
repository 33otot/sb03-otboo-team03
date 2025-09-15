package com.samsamotot.otboo.user.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AuthorDto(
    UUID userId,
    String name,
    String profileImageUrl
) {

}
