package com.samsamotot.otboo.clothes.exception.definition;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import java.util.Map;

public class ClothesAttributeDefException extends OtbooException {

    protected ClothesAttributeDefException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected ClothesAttributeDefException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected ClothesAttributeDefException(ErrorCode errorCode, Map<String, String> details) {
        super(errorCode, details);
    }

    protected ClothesAttributeDefException(ErrorCode errorCode, String message, Map<String, String> details) {
        super(errorCode, message, details);
    }
}
