package com.samsamotot.otboo.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.samsamotot.otboo.common.S3ImageFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;

@ExtendWith(MockitoExtension.class)
class S3ImageServiceTest {
    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3ImageStorage s3ImageStorage;

    private final String bucketName = "test-bucket";
    private final String region = "ap-northeast-2";

    @BeforeEach
    void setUp() {
        s3ImageStorage = new S3ImageStorage(s3Client);

        ReflectionTestUtils.setField(s3ImageStorage, "bucketName", bucketName);
        ReflectionTestUtils.setField(s3ImageStorage, "region", region);
    }

    @Test
    void 이미지_S3_업로드에_성공하면_URL을_반환한다() {

        // given
        MockMultipartFile file = S3ImageFixture.createSampleImageFile();
        String folderPath = "profile/";

        // when
        String resultUrl = s3ImageStorage.uploadImage(file, folderPath);

        // then
        assertThat(resultUrl)
            .startsWith("https://" + bucketName + ".s3." + region + ".amazonaws.com/" + folderPath);
    }

    @Test
    void 파일이_비어있으면_BAD_REQUEST_예외를_던진다() {
        // given
        MockMultipartFile emptyFile = S3ImageFixture.createEmptyImageFile();

        assertThatThrownBy(() -> s3ImageStorage.uploadImage(emptyFile, "profile/"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("업로드할 파일이 없습니다.");
    }
}