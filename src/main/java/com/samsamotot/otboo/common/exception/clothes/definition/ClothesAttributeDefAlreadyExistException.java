package com.samsamotot.otboo.common.exception.clothes.definition;

import com.samsamotot.otboo.common.exception.ErrorCode;

public class ClothesAttributeDefAlreadyExistException extends ClothesAttributeDefException{

    public ClothesAttributeDefAlreadyExistException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ClothesAttributeDefAlreadyExistException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
