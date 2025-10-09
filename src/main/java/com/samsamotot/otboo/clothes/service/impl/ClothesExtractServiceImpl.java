package com.samsamotot.otboo.clothes.service.impl;

import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.service.ClothesExtractService;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClothesExtractServiceImpl implements ClothesExtractService {

    @Override
    public ClothesDto extract(String url) {

        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get();

            // 기능 확인: 무신사 기본 구조 기반 파싱
            String imageUrl = null;
            String name = null;
            String category = null;

            // 이미지 추출
            Element imgEl = doc.selectFirst("div.sc-uxvjgl-8 img");
            if (imgEl != null) {
                imageUrl = imgEl.attr("src");
            } else {
                // fallback: Open Graph
                imageUrl = doc.select("meta[property=og:image]").attr("content");
            }

            // ️의상 이름 추출
            Element nameEl = doc.selectFirst("span[data-mds=Typography]");
            if (nameEl != null) {
                name = nameEl.text();
            } else {
                name = doc.select("meta[property=og:title]").attr("content");
            }

            // 최상위 카테고리 추출
            Element categoryEl = doc.selectFirst("a[data-category-id=1depth]");
            if (categoryEl != null) {
                category = categoryEl.attr("data-category-name");
            }
            log.info("==========================================");
            log.info("최상위 카테고리 추출 결과: {}", category);
            log.info("==========================================");

            // 기본값 보정
            if (name.isEmpty())
                name = "(이름 없음)";
            if (imageUrl.isEmpty())
                imageUrl = "(이미지 없음)";
            if (category == null || category.isEmpty())
                category = "(카테고리 없음)";

            ClothesDto returnDto = new ClothesDto(
                null,
                null,
                name,
                imageUrl,
                null,
                null
            );

            return returnDto;

        } catch (IOException e) {
            log.error("스크래핑 실패: {}", e.getMessage());
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
