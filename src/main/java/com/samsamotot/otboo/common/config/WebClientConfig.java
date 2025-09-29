package com.samsamotot.otboo.common.config;

import com.samsamotot.otboo.location.client.KakaoApiClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
public class WebClientConfig {

    @Value("${kakao.api.base-url}")
    private String baseUrl;

    @Value("${kma.alt-url}")
    private String kmaBaseUrl;

    /**
     * WebClient를 위한 공통 HttpClient 설정을 생성합니다.
     * - Connection Timeout: 5초
     * - Response Timeout: 5초 (전체 응답 시간)
     * - Read/Write Timeout: 5초 (데이터를 읽고 쓰는 시간)
     * @return 구성된 HttpClient 인스턴스
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.create()
                // Connection Timeout
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                // Response Timeout
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS)) // Read Timeout
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));
    }

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
    public WebClient kakaoWebClient(HttpClient httpClient) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    @Qualifier("kmaWebClient")
    public WebClient kmaWebClient(HttpClient httpClient) {
        return WebClient.builder()
                .baseUrl(kmaBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
