package com.samsamotot.otboo.common.email;

/**
 * 이메일 서비스 인터페이스
 */
public interface EmailService {
    
    /**
     * 임시 비밀번호 이메일 발송
     * 
     * @param email 이메일 주소
     * @param temporaryPassword 임시 비밀번호
     */
    void sendTemporaryPassword(String email, String temporaryPassword);
}
