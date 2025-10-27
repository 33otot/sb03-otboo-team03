package com.samsamotot.otboo.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.samsamotot.otboo.common.fixture.S3ImageFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageTest {
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

    @Nested
    @DisplayName("S3 이미지 업로드 테스트")
    class S3UploadTest {

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
        void 파일이_null이면_BAD_REQUEST_예외가_발생한다() {
            assertThatThrownBy(() -> s3ImageStorage.uploadImage(null, "profile/"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("업로드할 파일이 없습니다.");
        }

        @Test
        void 파일이_비어있으면_BAD_REQUEST_예외를_던진다() {
            // given
            MockMultipartFile emptyFile = S3ImageFixture.createEmptyImageFile();

            assertThatThrownBy(() -> s3ImageStorage.uploadImage(emptyFile, "profile/"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("업로드할 파일이 없습니다.");
        }

        @Test
        void folderPath가_비어있으면_BAD_REQUEST_예외가_발생한다() {
            MockMultipartFile file = S3ImageFixture.createSampleImageFile();

            assertThatThrownBy(() -> s3ImageStorage.uploadImage(file, ""))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("S3 업로드 경로(folderPath)가 비어있습니다.");
        }

        @Test
        void originalFilename이_null이면_기본_파일명으로_처리된다() {
            // given
            MockMultipartFile fileNoName = new MockMultipartFile(
                "file",
                null,
                "image/jpeg",
                "test-image-content".getBytes()
            );
            String folderPath = "clothes/";

            // when
            String resultUrl = s3ImageStorage.uploadImage(fileNoName, folderPath);

            // then
            assertThat(resultUrl).isNotNull();
            assertThat(resultUrl).startsWith("https://" + bucketName + ".s3." + region + ".amazonaws.com/" + folderPath);
        }

        @Test
        void originalFilename이_빈_문자열이면_기본_파일명으로_처리된다() {
            // given
            MockMultipartFile emptyNameFile = new MockMultipartFile(
                "file",
                "",  // originalFilename이 빈 문자열
                "image/jpeg",
                "test-image-content".getBytes()
            );
            String folderPath = "clothes/";

            // when
            String resultUrl = s3ImageStorage.uploadImage(emptyNameFile, folderPath);

            // then
            assertThat(resultUrl).isNotNull();
            assertThat(resultUrl).startsWith("https://" + bucketName + ".s3." + region + ".amazonaws.com/" + folderPath);
        }

        @Test
        @DisplayName("originalFilename이 공백이면 defaultImage.jpg로 처리된다")
        void originalFilename이_공백이면_기본_파일명으로_처리된다() {
            // given
            MockMultipartFile blankNameFile = new MockMultipartFile(
                "file",
                "   ",
                "image/jpeg",
                "test-image-content".getBytes()
            );
            String folderPath = "clothes/";

            // when
            String resultUrl = s3ImageStorage.uploadImage(blankNameFile, folderPath);

            // then
            assertThat(resultUrl).isNotNull();
            assertThat(resultUrl).startsWith("https://" + bucketName + ".s3." + region + ".amazonaws.com/" + folderPath);
        }

        @Test
        void createFileName에_null을_전달하면_예외가_발생한다() {
            // given
            String nullFilename = null;

            // when & then
            assertThatThrownBy(() -> s3ImageStorage.createFileName(nullFilename))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void extracted_prefix가_있으면_파일명을_그대로_반환한다() {
            // given
            String extractedFilename = "extracted-169583ce-b68a-433a-b95a-98a7cb276c88.webp";

            // when
            String result = s3ImageStorage.createFileName(extractedFilename);

            // then
            assertThat(result).isEqualTo(extractedFilename);
            assertThat(result).startsWith("extracted-");
        }

        @Test
        void 일반_파일명은_UUID로_재생성된다() {
            // given
            String normalFilename = "myimage.jpg";

            // when
            String result = s3ImageStorage.createFileName(normalFilename);

            // then
            assertThat(result).isNotEqualTo(normalFilename);
            assertThat(result).doesNotStartWith("extracted-");
            assertThat(result).endsWith(".jpg");
        }
    }

    @Nested
    @DisplayName("S3 이미지 삭제 테스트")
    class S3DeleteTest {

        @Test
        void 유효한_URL이면_S3에서_이미지를_삭제한다() {
            // given
            MockMultipartFile file = S3ImageFixture.createSampleImageFile();
            String folderPath = "profile/";
            String imageUrl = s3ImageStorage.uploadImage(file, folderPath);

            // when
            s3ImageStorage.deleteImage(imageUrl);

            // then
            verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        void URL이_null이면_S3삭제를_호출하지_않는다() {
            // when
            s3ImageStorage.deleteImage(null);

            // then
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        void URL이_비어있으면_S3삭제를_호출하지_않는다() {
            // when
            s3ImageStorage.deleteImage("");

            // then
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        void 유효하지_않은_URL이면_RuntimeException을_던진다() {
            // given
            String invalidUrl = "https://wrong-bucket.s3.ap-northeast-2.amazonaws.com/profile/test.jpg";

            // when & then
            assertThatThrownBy(() -> s3ImageStorage.deleteImage(invalidUrl))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미지 삭제 중 오류가 발생했습니다.");
        }

        @Test
        void S3_삭제_중_Exception_발생시_SERVICE_UNAVAILABLE_예외를_던진다() {
            // given
            MockMultipartFile file = S3ImageFixture.createSampleImageFile();
            String folderPath = "profile/";
            String imageUrl = s3ImageStorage.uploadImage(file, folderPath);

            // s3Client.deleteObject()에서 예외 발생하도록 설정
            doThrow(new RuntimeException("S3 삭제 오류"))
                .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

            // when & then
            assertThatThrownBy(() -> s3ImageStorage.deleteImage(imageUrl))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("S3 이미지 삭제 중 외부 서비스 오류가 발생했습니다.")
                .matches(ex -> ((ResponseStatusException) ex).getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}