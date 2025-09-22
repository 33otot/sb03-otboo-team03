package com.samsamotot.otboo.user.exception;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;

public class UserNotFoundException extends OtbooException {

    public UserNotFoundException(String email) {
        super(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: ");
    }

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
