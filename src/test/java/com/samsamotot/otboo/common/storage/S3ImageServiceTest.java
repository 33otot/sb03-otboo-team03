package com.samsamotot.otboo.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

class S3ImageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3ImageService s3ImageService;

    private final String bucketName = "test-bucket";
    private final String region = "ap-northeast-2";

    @Test
    void 이미지_S3_업로드에_성공하면_URL을_반환한다() {

        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            "dummy-image-content".getBytes()
        );
        String folderPath = "profile/";

        // when
        String resultUrl = s3ImageService.uploadImage(file, folderPath);

        // then
        assertThat(resultUrl)
            .startsWith("https://" + bucketName + ".s3." + region + ".amazonaws.com/" + folderPath);
    }
}