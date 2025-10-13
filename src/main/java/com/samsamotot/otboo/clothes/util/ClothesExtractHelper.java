package com.samsamotot.otboo.clothes.util;

import com.samsamotot.otboo.clothes.exception.ClothesExtractionFailedException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class ClothesExtractHelper {
    private static final String SERVICE_NAME = "[ClothesExtractHelper] ";

    // 허용되는 프로토콜
    private static final List<String> ALLOWED_PROTOCOLS = List.of("http", "https");

    // 허용되는 포트 (80, 443만 허용)
    private static final List<Integer> ALLOWED_PORTS = List.of(80, 443, -1); // -1은 기본 포트

    // 허용할 도메인 (화이트리스트)
    private static final List<String> ALLOWED_DOMAINS = List.of(
        "musinsa.com",          // 무신사
        "a-bly.com",            // 에이블리
        "applink.a-bly.com",
        "shopping.naver.com",   // 네이버 쇼핑
        "shop-phinf.pstatic.net",
        "zigzag.kr",
        "29cm.co.kr"
    );

    // 사설 IP 대역 (SSRF 방어)
    private static final List<String> PRIVATE_IP_RANGES = List.of(
        "127.", // 127.0.0.0/8 - localhost
        "10.",  // 10.0.0.0/8 - 사설망
        "192.168.", // 192.168.0.0/16 - 사설망
        "169.254.", // 169.254.0.0/16 - 링크 로컬
        "0."    // 0.0.0.0/8 - 예약됨
    );

    // 에이블리처럼 Cloudflare 보호가 있는 사이트는 Jsoup.connect()만으로는 안 됨 (403 뜸)
    // Jsoup 대신 쿠키/헤더/Referer까지 세팅해서 HTTP 클라이언트(OkHttp / Jsoup+Cookies) 로 접근하기
    public String fetchHtml(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Referer", "https://m.a-bly.com/")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ko,en;q=0.9")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("HTTP error: " + response.code());
            return response.body().string();
        }
    }

    /**
     * URL의 전체 유효성을 검증
     * SSRF 공격을 방어하기 위한 검증
     *
     * @param urlString 검증할 URL 문자열
     * @throws ResponseStatusException URL이 유효하지 않거나 보안 위험이 있는 경우
     */
    public void validate(String urlString) {
        log.info(SERVICE_NAME + "URL 검증 시작: {}", urlString);

        // 1. URL 파싱
        URL url = parseUrl(urlString);

        // 2. 도메인 화이트리스트 검증
        validateDomainWhitelist(url);

        // 3. 프로토콜 검증
        validateProtocol(url);

        // 4. 포트 검증
        validatePort(url);

        // 5. DNS 해석 후 IP 검증 (SSRF 핵심 방어)
        validateResolvedIp(url);

        log.info(SERVICE_NAME + "URL 검증 성공: {}", urlString);
    }

    /**
     * URL 문자열을 파싱하고 기본 형식을 검증
     */
    private URL parseUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            log.warn(SERVICE_NAME + "빈 URL 입력됨");
            throw new ClothesExtractionFailedException(
                "URL이 비어있습니다."
            );
        }

        try {
            // URI로 먼저 검증 (더 엄격한 검증)
            URI uri = new URI(urlString);

            // URL로 변환
            URL url = uri.toURL();

            // 호스트가 없는 경우 차단
            if (url.getHost() == null || url.getHost().isEmpty()) {
                throw new ClothesExtractionFailedException(
                    "유효하지 않은 URL 형식입니다."
                );
            }

            return url;

        } catch (URISyntaxException e) {
            log.warn(SERVICE_NAME + "URI 파싱 실패: {}", e.getMessage());
            throw new ClothesExtractionFailedException(
                "URL 형식이 올바르지 않습니다."
            );
        } catch (MalformedURLException e) {
            log.warn(SERVICE_NAME + "URL 파싱 실패: {}", e.getMessage());
            throw new ClothesExtractionFailedException(
                "URL 형식이 올바르지 않습니다."
            );
        }
    }

    /**
     * 프로토콜이 허용된 목록에 있는지 검증
     * http, https만 허용하여 file://, ftp:// 등 위험한 프로토콜 차단
     */
    private void validateProtocol(URL url) {
        String protocol = url.getProtocol().toLowerCase();

        if (!ALLOWED_PROTOCOLS.contains(protocol)) {
            log.warn(SERVICE_NAME + "허용되지 않은 프로토콜: {}", protocol);
            throw new ClothesExtractionFailedException(
                "허용되지 않는 프로토콜 입니다."
            );
        }
    }

    /**
     * 포트가 허용된 범위 내에 있는지 검증
     * 80, 443 또는 기본 포트만 허용
     */
    private void validatePort(URL url) {
        int port = url.getPort();

        // 포트가 명시되지 않은 경우 기본 포트 사용 (-1)
        if (port == -1) {
            port = url.getDefaultPort();
        }

        if (!ALLOWED_PORTS.contains(port)) {
            log.warn(SERVICE_NAME + "허용되지 않은 포트: {}", port);
            throw new ClothesExtractionFailedException(
                "허용되지 않은 포트입니다."
            );
        }
    }

    /**
     * 도메인이 화이트리스트에 포함되어 있는지 검증
     */
    private void validateDomainWhitelist(URL url) {
        String host = url.getHost().toLowerCase();

        // 허용된 도메인인지 확인
        boolean isAllowed = false;
        for (String allowedDomain : ALLOWED_DOMAINS) {
            if (host.equals(allowedDomain) || host.endsWith("." + allowedDomain)) {
                isAllowed = true;
                log.info(SERVICE_NAME + "허용된 도메인 확인: {}", host);
                break;
            }
        }

        // 허용되지 않은 도메인이면 차단
        if (!isAllowed) {
            log.warn(SERVICE_NAME + "허용되지 않은 도메인 접근 시도: {}", host);
            throw new ClothesExtractionFailedException(
                "지원하지 않는 사이트입니다."
            );
        }
    }

    /**
     * DNS를 해석하여 실제 IP를 확인하고, 사설 IP 대역인지 검증
     * SSRF 공격의 핵심 방어 로직!!
     */
    private void validateResolvedIp(URL url) {
        String host = url.getHost();

        try {
            // DNS 해석하여 실제 IP 주소 얻기
            InetAddress inetAddress = InetAddress.getByName(host);
            String ipAddress = inetAddress.getHostAddress();

            log.info(SERVICE_NAME + "DNS 해석 결과 - 호스트: {}, IP: {}", host, ipAddress);

            // 1. 루프백 주소 차단 (127.0.0.1 등)
            if (inetAddress.isLoopbackAddress()) {
                log.warn(SERVICE_NAME + "루프백 주소 차단: {}", ipAddress);
                throw new ClothesExtractionFailedException(
                    "내부 네트워크에 접근할 수 없습니다."
                );
            }

            // 2. 링크 로컬 주소 차단 (169.254.x.x)
            if (inetAddress.isLinkLocalAddress()) {
                log.warn(SERVICE_NAME + "링크 로컬 주소 차단: {}", ipAddress);
                throw new ClothesExtractionFailedException(
                    "내부 네트워크에 접근할 수 없습니다."
                );
            }

            // 3. 사이트 로컬 주소 차단 (사설 IP: 10.x.x.x, 192.168.x.x, 172.16-31.x.x)
            if (inetAddress.isSiteLocalAddress()) {
                log.warn(SERVICE_NAME + "사설 IP 주소 차단: {}", ipAddress);
                throw new ClothesExtractionFailedException(
                    "내부 네트워크에 접근할 수 없습니다."
                );
            }

            // 4. 추가 사설 IP 대역 검증 (172.16.0.0/12 범위 포함)
            if (isPrivateIpRange(ipAddress)) {
                log.warn(SERVICE_NAME + "사설 IP 대역 차단: {}", ipAddress);
                throw new ClothesExtractionFailedException(
                    "내부 네트워크에 접근할 수 없습니다."
                );
            }

            // 5. AWS 메타데이터 서버 차단
            if (ipAddress.equals("54.180.118.154")) {
                log.warn(SERVICE_NAME + "AWS 메타데이터 서버 접근 차단: {}", ipAddress);
                throw new ClothesExtractionFailedException(
                    "접근이 제한된 주소입니다."
                );
            }

        } catch (UnknownHostException e) {
            log.warn(SERVICE_NAME + "DNS 해석 실패: {}", host);
            throw new ClothesExtractionFailedException(
                "유효하지 않은 도메인입니다."
            );
        }
    }

    /**
     * IP 주소가 사설 IP 대역에 속하는지 추가 검증
     * 172.16.0.0/12 대역 등 InetAddress가 놓칠 수 있는 범위 포함
     */
    private boolean isPrivateIpRange(String ipAddress) {
        // 기본 사설 IP 접두사 검증
        for (String privateRange : PRIVATE_IP_RANGES) {
            if (ipAddress.startsWith(privateRange)) {
                return true;
            }
        }

        // 172.16.0.0/12 대역 검증 (172.16.x.x ~ 172.31.x.x)
        if (ipAddress.startsWith("172.")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length >= 2) {
                try {
                    int secondOctet = Integer.parseInt(parts[1]);
                    if (secondOctet >= 16 && secondOctet <= 31) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // 파싱 실패 시 안전하게 통과
                }
            }
        }

        return false;
    }

    /**
     * URL이 리다이렉트 응답인지 검증
     * HTTP 응답 코드 3xx를 차단하는 용도로 사용
     *
     * @param responseCode HTTP 응답 코드
     * @return 리다이렉트 응답이면 true
     */
    public boolean isRedirectResponse(int responseCode) {
        return responseCode >= 300 && responseCode < 400;
    }
}
