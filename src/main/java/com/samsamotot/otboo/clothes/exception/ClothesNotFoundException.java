package com.samsamotot.otboo.clothes.exception;

import com.samsamotot.otboo.common.exception.ErrorCode;

public class ClothesNotFoundException extends ClothesException {

    public ClothesNotFoundException() {
        super(ErrorCode.CLOTHES_NOT_FOUND);
    }
}
