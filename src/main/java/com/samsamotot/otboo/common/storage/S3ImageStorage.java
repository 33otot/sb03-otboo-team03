package com.samsamotot.otboo.common.storage;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3 버킷에서 이미지를 관리하기 위한 컴포넌트
 */
@Slf4j
@ConditionalOnProperty(name = "otboo.storage.type", havingValue = "s3")
@Component
public class S3ImageStorage {
    private static final String CLASS_NAME = "[S3ImageStorage] ";

    private final S3Client s3Client;

    @Value("${otboo.storage.s3.bucket}")
    private String bucketName;

    @Value("${otboo.storage.s3.region}")
    private String region;

    public S3ImageStorage(S3Client s3Client) {
        this.s3Client = s3Client;
    }


    // ========== 이미지 업로드 로직 ========== //

    public String uploadImage(MultipartFile file, String folderPath) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다.");
        }

        // 폴더 경로 검증
        if (folderPath == null || folderPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "S3 업로드 경로(folderPath)가 비어있습니다.");
        }

        String originalFileName = file.getOriginalFilename();

        // 원본 파일명이 없거나 비어있으면 기본 이름 사용
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "defaultImage.jpg";
        }
        String fileName = createFileName(originalFileName);

        // 경로 설정
        String s3Key = folderPath + fileName;

        // 요청 객체 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();

        try {
            s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            log.info(CLASS_NAME + "이미지 업로드 성공 - 경로: {}", s3Key);

            // 업로드된 객체의 공개 URL 반환
            return generatePublicUrl(s3Key);

        } catch (S3Exception | SdkClientException e) {
            log.error(CLASS_NAME + "S3 업로드 실패 - 경로: {}, 오류: {}", s3Key, e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "S3 업로드 중 외부 서비스 오류가 발생했습니다."
            );
        } catch (IOException e) {
            log.error(CLASS_NAME + "이미지 업로드 실패 - 파일명: {}, 오류: {}", originalFileName, e.getMessage(), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "이미지 업로드 중 오류가 발생했습니다."
            );
        }
    }

    // 파일명을 난수화하기 위해 UUID 를 활용하여 난수를 돌린다.
    public String createFileName(String fileName){
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }

    //  "."의 존재 유무만 판단
    private String getFileExtension(String fileName){
        try{
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일(" + fileName + ") 입니다.");
        }
    }

    // 퍼블릭 URL 생성
    private String generatePublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }


    // ========== 이미지 제거 로직 ========== //
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            log.warn(CLASS_NAME + "삭제하려는 이미지의 URL이 비어 있습니다.");
            return;
        }

        try {
            String s3Key = extractKey(imageUrl);
            log.info(CLASS_NAME + "삭제하려는 이미지: {}", s3Key);

            // 요청 객체 생성
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            try{
                log.debug(CLASS_NAME + "이미지 삭제 작업 시작");
                s3Client.deleteObject(deleteObjectRequest);
                log.debug(CLASS_NAME + "이미지 삭제 성공");
            }
            catch (Exception e) {
                log.error(CLASS_NAME + "이미지 삭제 작업 실패, 오류 발생: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 이미지 삭제 중 외부 서비스 오류가 발생했습니다.");
            }
        }
        catch (IllegalArgumentException e) {
            log.error(CLASS_NAME + "S3 key 추출 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 삭제 중 오류가 발생했습니다.", e);
        }
    }

    // s3 key 추출 메서드
    private String extractKey(String url) {
        String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (!url.startsWith(baseUrl)) {
            throw new IllegalArgumentException("유효한 S3 공개 URL이 아닙니다: " + url);
        }
        String key = url.substring(baseUrl.length());
        if (key.isEmpty()) {
            throw new IllegalArgumentException("S3 key가 비어 있습니다: " + url);
        }
        return key;
    }

}
