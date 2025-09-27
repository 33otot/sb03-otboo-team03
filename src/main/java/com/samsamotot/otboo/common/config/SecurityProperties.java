package com.samsamotot.otboo.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 보안 관련 설정 프로퍼티
 */
@Data
@Component
@ConfigurationProperties(prefix = "otboo.security")
public class SecurityProperties {
  
  /**
   * 쿠키 관련 설정
   */
  private Cookie cookie = new Cookie();
  
  @Data
  public static class Cookie {
    /**
     * 쿠키의 Secure 플래그 (HTTPS에서만 전송)
     */
    private boolean secure = false;
    
    /**
     * 쿠키의 SameSite 속성 (Lax, Strict, None)
     */
    private String sameSite = "Lax";
  }
}
