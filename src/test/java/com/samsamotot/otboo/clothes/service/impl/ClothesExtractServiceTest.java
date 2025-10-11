package com.samsamotot.otboo.clothes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsamotot.otboo.clothes.dto.request.ClothesDto;
import com.samsamotot.otboo.clothes.util.ImageDownloadService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClothesExtractServiceTest {

    @Mock
    private ImageDownloadService imageDownloadService;

    @InjectMocks
    @Spy
    private ClothesExtractServiceImpl clothesExtractService;

    private static final String VALID_HTML = """
        <html>
          <head>
            <meta property="og:title" content="[무신사] 베이직 티셔츠">
            <meta property="og:image" content="https://image.musinsa.com/images/product.jpg">
          </head>
          <body>
            <div class="sc-uxvjgl-8">
              <img src="https://image.musinsa.com/images/product.jpg?size=500">
            </div>
            <span data-mds="Typography">[무신사] 베이직 티셔츠</span>
          </body>
        </html>
        """;

    private static final String ABLY_HTML = """
    <html>
      <head>
        <meta property="og:title" content="[에이블리] 플라워 원피스">
        <meta property="og:image" content="https://image.a-bly.com/images/product.jpg">
      </head>
      <body>
        <div class="sc-uxvjgl-8">
          <img src="https://image.a-bly.com/images/product.jpg?size=500">
        </div>
        <span data-mds="Typography">[에이블리] 플라워 원피스</span>
      </body>
    </html>
""";

    private static final String INVALID_HTML = """
        <html><body><p>403 Forbidden</p></body></html>
        """;


    @Test
    void 정상적인_HTML에서_의상_이름과_이미지를_추출할_수_있다() throws Exception {
        // given
        String url = "https://store.musinsa.com/product/12345";
        doReturn(VALID_HTML).when(clothesExtractService).fetchHtml(anyString());

        // when
        ClothesDto dto = clothesExtractService.extract(url);

        // then
        assertNotNull(dto);
        assertEquals("[무신사] 베이직 티셔츠", dto.name());
        assertTrue(dto.imageUrl().startsWith("https://"));
        verify(imageDownloadService, never()).downloadAndUploadAsync(any(), any());
    }

    @Test
    void 에이블리_링크의_경우_이미지_업로드가_수행되고_S3_URL이_반환된다() throws Exception {

        // given
        String url = "https://a-bly.com/products/98765";

        // HTML 파싱 결과를 Mock으로 대체
        doReturn(ABLY_HTML).when(clothesExtractService).fetchHtml(anyString());

        // S3 업로드 Mock (즉시 완료 future 리턴)
        String expectedS3Url = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/clothes/ably98765.jpg";
        CompletableFuture<String> mockFuture = CompletableFuture.completedFuture(expectedS3Url);
        when(imageDownloadService.downloadAndUploadAsync(anyString(), eq("clothes/"))).thenReturn(mockFuture);

        // when
        ClothesDto dto = clothesExtractService.extract(url);

        // then
        assertNotNull(dto);
        assertEquals("[에이블리] 플라워 원피스", dto.name());
        assertEquals(expectedS3Url, dto.imageUrl());  // mockFuture 결과가 반영되었는지 확인

        // verify
        verify(imageDownloadService, times(1))
            .downloadAndUploadAsync(anyString(), eq("clothes/"));
    }

    @Test
    void INVALID_HTML_리턴시_기본값_DTO를_반환한다() throws Exception {
        // given
        String url = "https://store.musinsa.com/product/invalid";
        doReturn(INVALID_HTML).when(clothesExtractService).fetchHtml(anyString());

        // when
        ClothesDto dto = clothesExtractService.extract(url);

        // then
        assertNotNull(dto);
        assertEquals("(이름 없음)", dto.name());
        assertNull(dto.imageUrl());
        verify(imageDownloadService, never()).downloadAndUploadAsync(any(), any());
    }

    @Test
    void IOException이_발생할_경우_에러_DTO를_반환한다() throws Exception {
        // given
        String url = "https://store.musinsa.com/product/99999";

        // fetchHtml()이 IOException 발생하도록 설정
        doThrow(new IOException("HTTP error: 403"))
            .when(clothesExtractService).fetchHtml(anyString());

        // when
        ClothesDto dto = clothesExtractService.extract(url);

        // then
        assertNotNull(dto);
        assertEquals("에러", dto.name());
        assertEquals("에러", dto.imageUrl());
        verify(imageDownloadService, never()).downloadAndUploadAsync(anyString(), anyString());
    }
}
