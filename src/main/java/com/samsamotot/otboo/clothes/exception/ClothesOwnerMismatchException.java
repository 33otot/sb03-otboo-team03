package com.samsamotot.otboo.clothes.exception;

import com.samsamotot.otboo.common.exception.ErrorCode;

public class ClothesOwnerMismatchException extends ClothesException {

    public ClothesOwnerMismatchException() {
        super(ErrorCode.CLOTHES_OWNER_MISMATCH);
    }
}
