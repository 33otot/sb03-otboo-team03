package com.samsamotot.otboo.user.exception;

import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.exception.ErrorCode;

/**
 * 유효성 검사 예외
 */
public class ValidationException extends OtbooException {
    
    public ValidationException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE, message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(ErrorCode.INVALID_INPUT_VALUE, message);
    }
}
