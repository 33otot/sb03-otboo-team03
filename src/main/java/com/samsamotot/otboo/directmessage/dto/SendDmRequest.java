package com.samsamotot.otboo.directmessage.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * PackageName  : com.samsamotot.otboo.directmessage.dto
 * FileName     : SendDmRequest
 * Author       : dounguk
 * Date         : 2025. 9. 26.
 */
public record SendDmRequest(
    @NotNull(message = "senderId는 필수입니다.")
    UUID senderId,

    @NotNull(message = "receiverId는 필수입니다.")
    UUID receiverId,

    @NotBlank(message = "쪽지 내용은 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "쪽지 내용은 최대 1000자까지 가능합니다.")
    String content
) {}
