package com.samsamotot.otboo.common.config;

import com.samsamotot.otboo.location.client.KakaoApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 카카오 API 설정을 위한 Configuration 클래스
 * 
 * <p>카카오 API 호출을 위한 WebClient 빈을 생성하고 설정합니다.
 * application.yaml의 kakao.api.base-url 속성을 사용하여
 * 카카오 API의 기본 URL을 설정합니다.</p>
 * 
 * <p>생성된 WebClient는 KakaoApiClient에서 주입받아 사용됩니다.</p>
 * 
 * @author HuInDoL
 * @since 1.0
 * @see WebClient
 * @see KakaoApiClient
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${kakao.api.base-url}")
    private String baseUrl;

    @Value("${kma.base-url}")
    private String kmaBaseUrl;

    /**
     * 카카오 API 호출을 위한 WebClient 빈을 생성합니다.
     * 
     * <p>생성된 WebClient는 다음과 같은 설정을 가집니다:</p>
     * <ul>
     *   <li>baseUrl: 카카오 API의 기본 URL (https://dapi.kakao.com)</li>
     *   <li>기본 HTTP 클라이언트 설정</li>
     * </ul>
     * 
     * <p>이 WebClient는 KakaoApiClient에서 주입받아 사용되며,
     * 카카오 로컬 API 호출에 사용됩니다.</p>
     * 
     * @return WebClient 카카오 API 호출용 WebClient 인스턴스
     * @see WebClient
     * @see KakaoApiClient
     * 
     * @since 1.0
     */
    @Bean
    @Qualifier("kakaoWebClient")
    public WebClient kakaoWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    @Qualifier("kmaWebClient")
    public WebClient kmaWebClient() {
        return WebClient.builder()
                .baseUrl(kmaBaseUrl)
                .build();
    }
}
