package com.samsamotot.otboo.clothes.exception.definition;

import com.samsamotot.otboo.common.exception.ErrorCode;

public class ClothesAttributeDefNotFoundException extends ClothesAttributeDefException {

    public ClothesAttributeDefNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ClothesAttributeDefNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
