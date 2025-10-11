package com.samsamotot.otboo.clothes.util;

import com.samsamotot.otboo.common.storage.S3ImageStorage;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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

    @Async("imageTaskExecutor")
    public CompletableFuture<String> downloadAndUploadAsync(String imageUrl, String folderPath) {
        try {

            URLConnection conn = new URL(imageUrl).openConnection();

            try (InputStream in = conn.getInputStream()) {
                byte[] bytes = in.readAllBytes();

                // 확장자 추출
                String ext = imageUrl.substring(imageUrl.lastIndexOf('.') + 1);
                MultipartFile file = new SimpleMultipartFile(
                    "file",
                    "downloaded." + ext,
                    "image/" + ext,
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