package com.samsamotot.otboo.common.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class OtbooException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Map<String, String> details;

    public OtbooException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public OtbooException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public OtbooException(ErrorCode errorCode, Map<String, String> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public OtbooException(ErrorCode errorCode, String message, Map<String, String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}
