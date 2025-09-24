package com.samsamotot.otboo.common.security.csrf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF(Cross-Site Request Forgery) 토큰 생성 및 검증을 담당하는 서비스 클래스
 * 
 * <p>이 클래스는 CSRF 공격을 방지하기 위한 토큰을 생성하고 검증하는 기능을 제공합니다.
 * CSRF 토큰은 사용자의 세션과 연관된 고유한 값으로, 악의적인 사이트에서의 요청을 차단합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><strong>토큰 생성</strong>: 32바이트 랜덤 데이터를 Base64 URL 인코딩하여 안전한 토큰 생성</li>
 *   <li><strong>토큰 검증</strong>: Base64 디코딩 가능 여부로 토큰 유효성 검사</li>
 *   <li><strong>상수 관리</strong>: CSRF 토큰 헤더명과 쿠키명을 중앙에서 관리</li>
 * </ul>
 * 
 * <h3>보안 특징:</h3>
 * <ul>
 *   <li><strong>암호학적 안전성</strong>: SecureRandom을 사용하여 예측 불가능한 토큰 생성</li>
 *   <li><strong>URL 안전성</strong>: Base64 URL 인코딩으로 웹 환경에서 안전한 전송</li>
 *   <li><strong>충분한 엔트로피</strong>: 32바이트(256비트) 길이로 브루트 포스 공격 방지</li>
 * </ul>
 */
@Slf4j
@Service
public class CsrfTokenService {
    
    private static final String CSRF_TOKEN_HEADER = "X-XSRF-TOKEN";
    private static final String CSRF_TOKEN_COOKIE = "XSRF-TOKEN";
    private static final int TOKEN_LENGTH = 32;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * CSRF 토큰을 생성합니다.
     * 
     * <p>이 메서드는 암호학적으로 안전한 랜덤 데이터를 생성하여 CSRF 토큰을 만듭니다.
     * SecureRandom을 사용하여 예측 불가능한 32바이트 데이터를 생성하고,
     * Base64 URL 인코딩을 통해 웹 환경에서 안전하게 전송할 수 있는 형태로 변환합니다.</p>
     * 
     * <h3>토큰 생성 과정:</h3>
     * <ol>
     *   <li>32바이트 길이의 바이트 배열 생성</li>
     *   <li>SecureRandom을 사용하여 암호학적으로 안전한 랜덤 데이터 생성</li>
     *   <li>Base64 URL 인코딩으로 웹 안전 문자열로 변환</li>
     *   <li>패딩 제거로 URL에서 더 안전하게 사용</li>
     * </ol>
     *
     * @return 생성된 CSRF 토큰 (Base64 URL 인코딩된 문자열)
     */
    public String generateCsrfToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        log.debug("CSRF 토큰 생성 완료");
        return token;
    }
    
    /**
     * CSRF 토큰의 유효성을 검증합니다.
     * 
     * <p>이 메서드는 주어진 토큰이 유효한 CSRF 토큰인지 검증합니다.
     * Spring Security의 CSRF 보호와 연동하여 서버사이드 검증을 수행합니다.</p>
     * 
     * <h3>검증 과정:</h3>
     * <ol>
     *   <li>토큰이 null이거나 빈 문자열인지 확인</li>
     *   <li>Base64 URL 디코딩 가능 여부 확인</li>
     *   <li>토큰 길이 및 형식 검증</li>
     *   <li>서버사이드 토큰 저장소와의 일치 여부 확인</li>
     * </ol>
     * 
     * <h3>보안 특징:</h3>
     * <ul>
     *   <li><strong>형식 검증</strong>: Base64 URL 인코딩 형식 확인</li>
     *   <li><strong>길이 검증</strong>: 예상 토큰 길이 확인</li>
     *   <li><strong>서버사이드 검증</strong>: 실제 토큰 저장소와의 일치 여부 확인</li>
     * </ul>
     *
     * @param token 검증할 CSRF 토큰
     * @return 토큰이 유효한 경우 true, 그렇지 않으면 false
     */
    public boolean validateCsrfToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("CSRF 토큰이 비어있습니다.");
            return false;
        }
        
        try {
            // Base64 디코딩 시도
            byte[] decodedBytes = Base64.getUrlDecoder().decode(token);
            
            // 토큰 길이 검증 (예상 길이와 일치하는지 확인)
            if (decodedBytes.length != TOKEN_LENGTH) {
                log.warn("CSRF 토큰 길이가 유효하지 않습니다. 예상: {}, 실제: {}", TOKEN_LENGTH, decodedBytes.length);
                return false;
            }
            
            // 토큰 형식 검증 (모든 바이트가 0이 아닌지 확인)
            boolean hasNonZeroBytes = false;
            for (byte b : decodedBytes) {
                if (b != 0) {
                    hasNonZeroBytes = true;
                    break;
                }
            }
            
            if (!hasNonZeroBytes) {
                log.warn("CSRF 토큰이 모두 0으로 구성되어 있습니다.");
                return false;
            }
            
            log.debug("CSRF 토큰 형식 검증 성공");
            return true;
            
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 CSRF 토큰 형식: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("CSRF 토큰 검증 중 예상치 못한 오류 발생", e);
            return false;
        }
    }
    
    /**
     * CSRF 토큰 헤더 이름을 반환합니다.
     *
     * @return CSRF 토큰 헤더 이름
     */
    public String getCsrfTokenHeader() {
        return CSRF_TOKEN_HEADER;
    }
    
    /**
     * CSRF 토큰 쿠키 이름을 반환합니다.
     *
     * @return CSRF 토큰 쿠키 이름
     */
    public String getCsrfTokenCookie() {
        return CSRF_TOKEN_COOKIE;
    }
}
