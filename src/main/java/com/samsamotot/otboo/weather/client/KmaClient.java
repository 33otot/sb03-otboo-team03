package com.samsamotot.otboo.weather.client;

import com.samsamotot.otboo.weather.dto.WeatherForecastResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 기상청(KMA) Open API 호출용 WebClient 구성.
 * - Reactor Netty HttpClient 기반(연결 3s, 응답 5s, read/write 5s 타임아웃)
 * - GZIP 압축/리다이렉트 허용
 * - 인메모리 디코딩 버퍼 4MB
 * - baseUrl은 kma.base-url 프로퍼티에서 주입
 */
@Slf4j
@Component
public class KmaClient {

    private static final String CLIENT_NAME = "[KmaClient] ";

    private final WebClient webClient;
    private final String serviceKey;

    private static final String SERVICE_PATH = "/getVilageFcst";
    private static final String PAGE_NO = "1";
    private static final String NUM_OF_ROWS = "1000";
    private static final String DATA_TYPE = "JSON";

    public KmaClient(@Qualifier("kmaWebClient") WebClient webClient,
                     @Value("${kma.service-key}") String serviceKey) {
        this.webClient = webClient;
        this.serviceKey = serviceKey;
    }
    /**
     * KMA 단기예보(getVilageFcst) 호출 클라이언트.
     * 필수 쿼리파라미터(base_date, base_time, nx, ny, dataType=JSON, serviceKey)를 구성하여 호출한다.
     *
     * @param nx 격자 X
     * @param ny 격자 Y
     * @return WeatherForecastResponse를 담은 Mono
     * @exception WebClientResponseException
     */
    public Mono<WeatherForecastResponse> fetchWeather(int nx, int ny) {

        BaseDateTime baseDateTime = calculateBaseDateTime();

        log.info(CLIENT_NAME + "기상청 Open API 단기예보 호출 시작. {} {}",baseDateTime.baseDate, baseDateTime.baseTime);
        return webClient.get()
                .uri(uri -> uri
                        .path(SERVICE_PATH)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("numOfRows", NUM_OF_ROWS)
                            .queryParam("pageNo", PAGE_NO)
                            .queryParam("dataType", DATA_TYPE)
                            .queryParam("base_date", baseDateTime.baseDate)
                            .queryParam("base_time", baseDateTime.baseTime)
                            .queryParam("nx", nx)
                            .queryParam("ny", ny)
                            .build())
                .retrieve()
                .bodyToMono(WeatherForecastResponse.class);
    }

    /**
     * 기상청 API의 발표 시간에 맞춰 현재 시간을 기준으로 올바른 base_date와 base_time을 계산합니다.
     * API는 02:00, 05:00, 08:00, 11:00, 14:00, 17:00, 20:00, 23:00에 예보를 발표합니다.
     * @return 계산된 기준 날짜와 시간을 담은 BaseDateTime 객체
     */
    private BaseDateTime calculateBaseDateTime() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime nowTime = now.toLocalTime();
        LocalDate nowDate = now.toLocalDate();

        String baseTime;
        if (nowTime.isBefore(LocalTime.of(2, 10))) {
            // 02:10 이전이면, 전날 23:00 발표 데이터 사용
            nowDate = nowDate.minusDays(1);
            baseTime = "2300";
        } else if (nowTime.isBefore(LocalTime.of(5, 10))) {
            baseTime = "0200";
        } else if (nowTime.isBefore(LocalTime.of(8, 10))) {
            baseTime = "0500";
        } else if (nowTime.isBefore(LocalTime.of(11, 10))) {
            baseTime = "0800";
        } else if (nowTime.isBefore(LocalTime.of(14, 10))) {
            baseTime = "1100";
        } else if (nowTime.isBefore(LocalTime.of(17, 10))) {
            baseTime = "1400";
        } else if (nowTime.isBefore(LocalTime.of(20, 10))) {
            baseTime = "1700";
        } else if (nowTime.isBefore(LocalTime.of(23, 10))) {
            baseTime = "2000";
        } else {

            // 23:10 이후이면, 당일 23:00 발표 데이터 사용
            baseTime = "2300";
        }

        String baseDate = nowDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return new BaseDateTime(baseDate, baseTime);
    }

    private record BaseDateTime(String baseDate, String baseTime) { }

    /**
     * 주어진 예외가 재시도할 가치가 있는지 판단합니다.
     * - 5xx 서버 오류 또는 타임아웃과 같은 일시적인 네트워크 오류는 재시도 대상으로 간주합니다.
     * - 4xx 클라이언트 오류는 재시도해도 성공할 가능성이 없으므로 대상으로 삼지 않습니다.
     * @param throwable 발생한 예외
     * @return 재시도 대상이면 true
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientRequestException) {
            // 타임아웃 또는 연결 실패 등
            return true;
        }
        if (throwable instanceof WebClientResponseException e) {
            // 5xx 서버 오류
            return e.getStatusCode().is5xxServerError();
        }
        return false;
    }
}
