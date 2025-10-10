package com.samsamotot.otboo.common.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이메일 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private static final String SERVICE_NAME = "[EmailServiceImpl] ";
    
    private final JavaMailSender mailSender;
    
    @Override
    public void sendTemporaryPassword(String email, String temporaryPassword) {
        log.info(SERVICE_NAME + "임시 비밀번호 이메일 발송 시도 - 이메일: {}", email);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[옷장을 부탁해] 임시 비밀번호 발급");
            message.setText(buildEmailContent(temporaryPassword));
            
            mailSender.send(message);
            
            log.info(SERVICE_NAME + "임시 비밀번호 이메일 발송 성공 - 이메일: {}", email);
        } catch (Exception e) {
            log.error(SERVICE_NAME + "임시 비밀번호 이메일 발송 실패 - 이메일: {}, 오류: {}", email, e.getMessage(), e);
            throw new OtbooException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
    
    /**
     * 이메일 내용 구성
     * 
     * @param temporaryPassword 임시 비밀번호
     * @return 이메일 내용
     */
    private String buildEmailContent(String temporaryPassword) {
        return String.format("""
            안녕하세요. 옷장을 부탁해입니다.
            
            요청하신 임시 비밀번호가 발급되었습니다.
            
            임시 비밀번호: %s
            
            ※ 임시 비밀번호는 3분 후 만료됩니다.
            ※ 보안을 위해 로그인 후 즉시 새로운 비밀번호로 변경해 주세요.
            
            감사합니다.
            """, temporaryPassword);
    }
}
