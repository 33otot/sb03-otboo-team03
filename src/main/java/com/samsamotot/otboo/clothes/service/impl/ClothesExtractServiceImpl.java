package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.service.ClothesExtractService;
import com.samsamotot.otboo.clothes.util.ImageDownloadService;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesExtractServiceImpl implements ClothesExtractService {
    private static final String SERVICE_NAME = "[ClothesExtractService] ";

    // Selector를 상수로 관리
    // 기본
    private static final String SELECTOR_IMAGE_DEFAULT = "div.sc-uxvjgl-8 img";
    private static final String SELECTOR_NAME_DEFAULT = "span[data-mds=Typography]";

    // 기타 사이트 fallback
    private static final String SELECTOR_OG_IMAGE = "meta[property=og:image]";
    private static final String SELECTOR_OG_TITLE = "meta[property=og:title]";

    // 지원하지 않는 쇼핑몰 주소 설정
    private static final List<String> UNSUPPORTED_DOMAINS = List.of(
        "smartstore.naver.com",
        "kream.co.kr"
    );

    private final ImageDownloadService imageDownloadService;

    @Override
    public ClothesDto extract(String url) {
        try {
            // 지원하지 않는 도메인 검사
            for (String domain : UNSUPPORTED_DOMAINS) {
                if (url.contains(domain)) {
                    log.warn(SERVICE_NAME + "{} - 지원하지 않는 도메인 감지, 스크래핑 차단됨", domain);
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "지원하지 않는 사이트입니다."
                    );
                }
            }

            // Cloudflare 우회용 HTML 요청
            String html = fetchHtml(url);
            Document doc = Jsoup.parse(html);

            // 무신사 기본 구조 기반 파싱
            String imageUrl = null;
            String name = null;

            // 이미지 추출
            Element imgEl = doc.selectFirst(SELECTOR_IMAGE_DEFAULT);
            if (imgEl != null) {
                imageUrl = imgEl.attr("src");
            } else {
                // fallback: Open Graph
                imageUrl = doc.select(SELECTOR_OG_IMAGE).attr("content");
            }

            // 쿼리 파라미터 제거
            if (imageUrl.contains("?")) {
                imageUrl = imageUrl.substring(0, imageUrl.indexOf("?"));
            }

            // ️의상 이름 추출
            Element nameEl = doc.selectFirst(SELECTOR_NAME_DEFAULT);
            if (nameEl != null) {
                name = nameEl.text();
            } else {
                name = doc.select(SELECTOR_OG_TITLE).attr("content");
            }

            log.info(SERVICE_NAME + "의상 이름 추출 결과: {}", name);
            log.info(SERVICE_NAME + "의상 이미지 추출 결과: {}", imageUrl);

            // 기본값 보정
            if (name.isEmpty())
                name = "(이름 없음)";
            if (imageUrl.isEmpty())
                imageUrl = null;

            // 에이블리 / 네이버 쇼핑 링크 여부 판별
            boolean isAblyLink = url.contains("a-bly.com") || url.contains("applink.a-bly.com");
            boolean isNaverShop = url.contains("shopping.naver") || url.contains("shop-phinf.pstatic.net");

            String finalImageUrl = imageUrl;

            if ((isAblyLink || isNaverShop) && finalImageUrl != null && !finalImageUrl.isBlank()) {
                String siteName = isAblyLink ? "에이블리" : "네이버쇼핑";
                log.info(SERVICE_NAME + "<{}> 비동기 이미지 업로드 시작 - {}", siteName, finalImageUrl);

                // 비동기 다운로드 + 업로드 실행
                CompletableFuture<String> futureS3Url =
                    imageDownloadService.downloadAndUploadAsync(finalImageUrl, "clothes/");

                String s3Url = futureS3Url.join();

                if (s3Url != null) {
                    finalImageUrl = s3Url;
                    log.info(SERVICE_NAME + "<{}> 비동기 이미지 업로드 성공: {}", siteName, s3Url);
                } else {
                    log.warn(SERVICE_NAME + "<{}> 비동기 이미지 업로드 실패 - 원본 URL로 대체됩니다: {}", siteName, imageUrl);
                }
            }

            ClothesDto returnDto = new ClothesDto(
                null,
                null,
                name,
                finalImageUrl,
                null,
                null
            );

            return returnDto;

        } catch (HttpStatusException e) {
            log.error(SERVICE_NAME + "HTTP 상태 오류: {}", e.getStatusCode());
            return new ClothesDto(
                null,
                null,
                "에러",
                "에러",
                null,
                null);

        } catch (IOException e) {
            log.error(SERVICE_NAME + "스크래핑 실패: {}", e.getMessage());
            return new ClothesDto(
                null,
                null,
                "에러",
                "에러",
                null,
                null);
        }
    }

    // 에이블리처럼 Cloudflare 보호가 있는 사이트는 Jsoup.connect()만으로는 안 됨 (403 뜸)
    // Jsoup 대신 쿠키/헤더/Referer까지 세팅해서 HTTP 클라이언트(OkHttp / Jsoup+Cookies) 로 접근하기
    protected String fetchHtml(String url) throws IOException {
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
}
