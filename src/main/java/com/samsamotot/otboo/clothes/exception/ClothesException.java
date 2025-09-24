package com.samsamotot.otboo.clothes.exception;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import java.util.Map;

public class ClothesException extends OtbooException {

    protected ClothesException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected ClothesException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected ClothesException(ErrorCode errorCode, Map<String, String> details) {
        super(errorCode, details);
    }

    protected ClothesException(ErrorCode errorCode, String message, Map<String, String> details) {
        super(errorCode, message, details);
    }
}
