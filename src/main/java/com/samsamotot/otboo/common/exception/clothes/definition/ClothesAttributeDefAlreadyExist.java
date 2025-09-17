package com.samsamotot.otboo.common.exception.clothes.definition;

import com.samsamotot.otboo.common.exception.ErrorCode;

public class ClothesAttributeDefAlreadyExist extends ClothesAttributeDefException{

    public ClothesAttributeDefAlreadyExist(ErrorCode errorCode) {
        super(errorCode);
    }

    public ClothesAttributeDefAlreadyExist(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
