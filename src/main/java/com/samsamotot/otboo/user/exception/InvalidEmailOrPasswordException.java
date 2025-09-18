package com.samsamotot.otboo.user.exception;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;

public class InvalidEmailOrPasswordException extends OtbooException {

    public InvalidEmailOrPasswordException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public InvalidEmailOrPasswordException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
