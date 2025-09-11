package com.samsamotot.otboo.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final String exceptionName;
    private final String message;
    private final String code;
    private final int status;
    private final LocalDateTime timestamp;
    private final Map<String, String> details;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
            .exceptionName(errorCode.name())
            .message(errorCode.getMessage())
            .code(errorCode.getCode())
            .status(errorCode.getHttpStatus().value())
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> details) {
        return ErrorResponse.builder()
            .exceptionName(errorCode.name())
            .message(errorCode.getMessage())
            .code(errorCode.getCode())
            .status(errorCode.getHttpStatus().value())
            .timestamp(LocalDateTime.now())
            .details(details)
            .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String customMessage) {
        return ErrorResponse.builder()
            .exceptionName(errorCode.name())
            .message(customMessage)
            .code(errorCode.getCode())
            .status(errorCode.getHttpStatus().value())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
