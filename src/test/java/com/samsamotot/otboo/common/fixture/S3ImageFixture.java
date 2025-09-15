package com.samsamotot.otboo.common.fixture;

import org.springframework.mock.web.MockMultipartFile;

/**
 * S3 이미지 업로드 테스트 용 파일 생성하는 Fixture 클래스
 */
public class S3ImageFixture {

    private static final String DEFAULT_FILE_NAME = "test.png";
    private static final String DEFAULT_CONTENT_TYPE = "image/png";
    private static final byte[] DEFAULT_CONTENT = "dummy-image-content".getBytes();

    // 정상 파일
    public static MockMultipartFile createSampleImageFile() {
        return new MockMultipartFile(
            "file",
            DEFAULT_FILE_NAME,
            DEFAULT_CONTENT_TYPE,
            DEFAULT_CONTENT
        );
    }

     // 비어있는 이미지 파일 Fixture
    public static MockMultipartFile createEmptyImageFile() {
        return new MockMultipartFile(
            "file",
            "empty.png",
            DEFAULT_CONTENT_TYPE,
            new byte[0]
        );
    }

     // 잘못된 Content-Type의 파일 Fixture
    public static MockMultipartFile createInvalidTypeFile() {
        return new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "dummy-text".getBytes()
        );
    }

}
