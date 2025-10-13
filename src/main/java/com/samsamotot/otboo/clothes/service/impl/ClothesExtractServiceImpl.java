package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.service.ClothesExtractService;
import com.samsamotot.otboo.clothes.util.ClothesExtractHelper;
import com.samsamotot.otboo.clothes.util.ImageDownloadService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

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

    private final ImageDownloadService imageDownloadService;
    private final ClothesExtractHelper clothesExtractHelper;

    @Override
    public ClothesDto extract(String url) {
        try {
            // SSRF 방어를 위한 URL 검증
            clothesExtractHelper.validate(url);

            // Cloudflare 우회용 HTML 요청
            String html = clothesExtractHelper.fetchHtml(url);
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
}
