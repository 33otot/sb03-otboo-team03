package com.samsamotot.otboo.location.client;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.location.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * 카카오 로컬 API를 호출하는 클라이언트 클래스
 * 
 * <p>카카오 로컬 API를 사용하여 좌표(경도, 위도)를 기반으로 행정구역 정보를 조회합니다.
 * WGS84 좌표계를 사용하여 좌표를 주소로 변환하는 기능을 제공합니다.</p>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * // KakaoApiClient를 주입받아 사용
 * @Autowired
 * private KakaoApiClient kakaoApiClient;
 * 
 * // 좌표로 주소 조회
 * Mono<KakaoAddressResponse> response = kakaoApiClient.getRegionByCoordinates(127.154, 36.54643);
 * }</pre>
 * 
 * @author HuInDoL
 * @since 1.0
 * @see KakaoAddressResponse
 * @see WebClient
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoApiClient {

    private final WebClient kakaoWebClient;

    @Value("${kakao.api.key}")
    private String apiKey;

    /**
     * 좌표를 기반으로 행정구역 정보를 조회합니다.
     * 
     * <p>카카오 로컬 API의 coord2regioncode 엔드포인트를 호출하여
     * WGS84 좌표계의 경도와 위도를 행정구역 정보로 변환합니다.</p>
     * 
     * <p>API 호출 전 다음 검증을 수행합니다:</p>
     * <ul>
     *   <li>API 키가 null이 아닌지 확인</li>
     *   <li>API 키가 빈 문자열이 아닌지 확인</li>
     *   <li>API 키가 기본값("your_kakao_api_key_here")이 아닌지 확인</li>
     * </ul>
     * 
     * <p>API 호출 실패 시 OtbooException을 발생시킵니다.</p>
     * 
     * @param longitude 경도 (x축 좌표, WGS84 좌표계)
     * @param latitude  위도 (y축 좌표, WGS84 좌표계)
     * @return Mono&lt;KakaoAddressResponse&gt; 행정구역 정보를 담은 응답 객체
     * @throws OtbooException API 키가 설정되지 않은 경우
     * @throws WebClientResponseException 카카오 API 호출 실패 시 (403 Forbidden 등)
     * 
     * @see <a href="https://developers.kakao.com/docs/latest/ko/local/dev-guide#coord-to-address">카카오 로컬 API 문서</a>
     * @see KakaoAddressResponse
     * 
     * @since 1.0
     */
    public Mono<KakaoAddressResponse> getRegionByCoordinates(double longitude, double latitude) {
        // API 키 유효성 검증
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("invalid_api_key")) {
            log.error("카카오 API 키가 설정되지 않았습니다. apiKey: {}", apiKey);
            return Mono.error(new OtbooException(ErrorCode.NO_KAKAO_KEY));
        }
        
        // API 키 일부만 로깅 (보안을 위해 전체 키는 로깅하지 않음)
        log.info("카카오 API 호출 - API 키: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

        return kakaoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/geo/coord2regioncode.json")
                        .queryParam("x", longitude)    // 경도가 x축
                        .queryParam("y", latitude)     // 위도가 y축
                        .queryParam("input_coord", "WGS84")     // WGS84 좌표계 명시
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .retrieve()
                .bodyToMono(KakaoAddressResponse.class);
    }
}