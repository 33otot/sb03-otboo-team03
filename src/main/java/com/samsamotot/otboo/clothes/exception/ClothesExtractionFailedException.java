package com.samsamotot.otboo.clothes.exception;

import com.samsamotot.otboo.common.exception.ErrorCode;

public class ClothesExtractionFailedException extends ClothesException {

    public ClothesExtractionFailedException() {
        super(ErrorCode.CLOTHES_EXTRACTION_FAILED);
    }

    public ClothesExtractionFailedException(String message) {
        super(ErrorCode.CLOTHES_EXTRACTION_FAILED, message);
    }
}
