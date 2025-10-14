package com.samsamotot.otboo.clothes.util;

import com.samsamotot.otboo.common.storage.S3ImageStorage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDownloadService {
    private static final String SERVICE_NAME = "[ImageDownloadService] ";

    private final S3ImageStorage s3ImageStorage;
    private final ClothesExtractHelper clothesExtractHelper;

    @Async("imageTaskExecutor")
    public CompletableFuture<String> downloadAndUploadAsync(String imageUrl, String folderPath) {
        int CONNECT_TIMEOUT_VALUE = 5000;
        int READ_TIMEOUT_VALUE = 10000;

        try {
            // URL 검증 먼저 진행
            clothesExtractHelper.validate(imageUrl);

            HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_VALUE);
            conn.setReadTimeout(READ_TIMEOUT_VALUE);
            conn.setInstanceFollowRedirects(false);

            // 응답 코드 검증 (리다이렉트 차단)
            int responseCode = conn.getResponseCode();
            if (clothesExtractHelper.isRedirectResponse(responseCode)) {
                throw new IOException("리다이렉트는 허용되지 않습니다: " + responseCode);
            }
            if (responseCode != 200) {
                throw new IOException("유효하지 않은 응답 코드: " + responseCode);
            }

            // Content-Type 검증 사전 수행
            String contentType = conn.getContentType();
            if (contentType == null ||
                (!contentType.startsWith("image/jpeg") &&
                    !contentType.startsWith("image/png") &&
                    !contentType.startsWith("image/webp"))) {
                throw new IOException("지원하지 않는 Content-Type: " + contentType);
            }

            try (InputStream in = conn.getInputStream()) {
                final int MAX_BYTES = 5 * 1024 * 1024; // 5MB
                byte[] buf = new byte[8192];
                int n, total = 0;
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > MAX_BYTES) {
                        throw new IOException("이미지 크기 초과: " + total + " bytes");
                    }
                    out.write(buf, 0, n);
                }
                byte[] bytes = out.toByteArray();

                // 확장자 결정
                String ext;

                // Content-Type 기반으로 우선 추출
                if (contentType.equalsIgnoreCase("image/jpeg")) {
                    ext = "jpg";
                } else if (contentType.equalsIgnoreCase("image/png")) {
                    ext = "png";
                } else if (contentType.equalsIgnoreCase("image/webp")) {
                    ext = "webp";
                } else {
                    ext = "bin";
                }

                String finalContentType = ("bin".equals(ext)) ? "application/octet-stream" : ("image/" + ("jpg".equals(ext) ? "jpeg" : ext));

                MultipartFile file = new SimpleMultipartFile(
                    "file",
                    "downloaded." + ext,
                    finalContentType,
                    bytes
                );

                // 업로드
                String s3Url = s3ImageStorage.uploadImage(file, folderPath);

                log.info(SERVICE_NAME + "UPLOAD SUCCESS: {} → {}", imageUrl, s3Url);
                return CompletableFuture.completedFuture(s3Url);
            }

        } catch (Exception e) {
            log.error(SERVICE_NAME + "UPLOAD FAILED: {}, errorMessage: ({})", imageUrl, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
}