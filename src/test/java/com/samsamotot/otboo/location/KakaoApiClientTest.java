package com.samsamotot.otboo.location;

import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;
import com.samsamotot.otboo.common.fixture.dto.KakaoAddressResponseFixture;
import com.samsamotot.otboo.location.client.KakaoApiClient;
import com.samsamotot.otboo.location.dto.KakaoAddressResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoApiClient 단위 테스트")
class KakaoApiClientTest {

    @Mock
    private WebClient kakaoWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private KakaoApiClient kakaoApiClient;

    private final String VALID_API_KEY = "test-api-key_54354524";
    private final String INVALID_API_KEY = "invalid_api_key";

    @BeforeEach
    void setUp() {
        // API 키 설정
        ReflectionTestUtils.setField(kakaoApiClient, "apiKey", VALID_API_KEY);
    }

    @Test
    @DisplayName("API 키가 null인 경우 예외가 발생한다.")
    void API_키가_NULL이면_예외() {
        // Given
        ReflectionTestUtils.setField(kakaoApiClient, "apiKey", null);
        double longitude = 126.9780;
        double latitude = 37.5665;

        // When
        // Then
        assertThatThrownBy(() ->
                kakaoApiClient.getRegionByCoordinates(longitude, latitude).block()
        )
                .isInstanceOf(OtbooException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KAKAO_KEY_NOT_FOUND);
    }

    @Test
    @DisplayName("API 키가 빈 문자열인 경우 예외가 발생한다")
    void API_키가_빈_문자열이면_예외() {
        // Given
        ReflectionTestUtils.setField(kakaoApiClient, "apiKey", "");
        double longitude = 126.9780;
        double latitude = 37.5665;

        // When
        // Then
        assertThatThrownBy(() ->
                kakaoApiClient.getRegionByCoordinates(longitude, latitude).block()
        )
                .isInstanceOf(OtbooException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KAKAO_KEY_NOT_FOUND);
    }

    @Test
    @DisplayName("API 키가 기본값인 경우 예외가 발생한다")
    void API_키가_기본값이면_예외() {
        // Given
        ReflectionTestUtils.setField(kakaoApiClient, "apiKey", INVALID_API_KEY);
        double longitude = 126.9780;
        double latitude = 37.5665;

        // When
        // Then
        assertThatThrownBy(() ->
                kakaoApiClient.getRegionByCoordinates(longitude, latitude).block()
        )
                .isInstanceOf(OtbooException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.KAKAO_KEY_NOT_FOUND);
    }

    @Test
    @DisplayName("유효한 API 키로 정상적인 좌표를 요청하면 성공한다")
    void 유효한_API_키로_정상_좌표_요청하면_성공() {
        // Given
        double longitude = 126.9780;
        double latitude = 37.5665;
        KakaoAddressResponse expectedResponse = KakaoAddressResponseFixture.createKakaoAddressResponse();

        when(kakaoWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoAddressResponse.class)).thenReturn(Mono.just(expectedResponse));

        // When
        KakaoAddressResponse actualResponse = kakaoApiClient.getRegionByCoordinates(longitude, latitude).block();

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getDocuments()).hasSize(1);
        assertThat(actualResponse.getDocuments().get(0).getRegion1DepthName()).isEqualTo("서울특별시");

        // Mock 호출 검증
        verify(kakaoWebClient).get();
        verify(requestHeadersUriSpec).uri((Function<UriBuilder, URI>) any());
        verify(requestHeadersSpec).header("Authorization", "KakaoAK " + VALID_API_KEY);
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(KakaoAddressResponse.class);
    }

    @Test
    @DisplayName("카카오 API 호출 시 403 Forbidden 에러가 발생하면 예외가 전파된다")
    void 카카오_API_호출하고_403_Forbidden_발생하면_예외_전파() {
        // Given
        double longitude = 126.9780;
        double latitude = 37.5665;
        WebClientResponseException exception = WebClientResponseException.create(
                403, "Forbidden", null, null, null
        );

        when(kakaoWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoAddressResponse.class)).thenReturn(Mono.error(exception));

        // When
        // Then
        assertThatThrownBy(() ->
                kakaoApiClient.getRegionByCoordinates(longitude, latitude).block()
        )
                .isInstanceOf(WebClientResponseException.class)
                .hasMessageContaining("403 Forbidden");
    }

    @Test
    @DisplayName("API 호출 시 올바른 URI와 파라미터가 전달된다")
    void API_호출하면_올바른_URI와_파라미터_전달() {
        // Given
        double longitude = 126.9780;
        double latitude = 37.5665;
        KakaoAddressResponse expectedResponse = KakaoAddressResponseFixture.createKakaoAddressResponse();

        when(kakaoWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((Function<UriBuilder, URI>) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KakaoAddressResponse.class)).thenReturn(Mono.just(expectedResponse));

        // When
        kakaoApiClient.getRegionByCoordinates(longitude, latitude).block();

        // Then

        verify(requestHeadersUriSpec).uri((Function<UriBuilder, URI>) any());
        verify(requestHeadersSpec).header("Authorization", "KakaoAK " + VALID_API_KEY);
    }
}
