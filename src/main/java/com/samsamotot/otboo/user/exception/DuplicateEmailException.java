package com.samsamotot.otboo.user.exception;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;

/**
 * 중복 이메일 예외
 */
public class DuplicateEmailException extends OtbooException {
    
    public DuplicateEmailException(String email) {
        super(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다: ");
    }
}