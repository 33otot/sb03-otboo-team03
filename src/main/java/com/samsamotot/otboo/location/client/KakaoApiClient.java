package com.samsamotot.otboo.location.client;

import com.samsamotot.otboo.location.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoApiClient {

    private final WebClient kakaoWebClient;

    @Value("${kakao.api.key}")
    private String apiKey;

    public Mono<KakaoAddressResponse> getRegionByCoordinates(double longitude, double latitude) {
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
