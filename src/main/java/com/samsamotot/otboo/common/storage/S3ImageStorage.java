package com.samsamotot.otboo.common.storage;

import java.io.IOException;
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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3 버킷에서 이미지를 관리하기 위한 컴포넌트
 */
@Slf4j
@ConditionalOnProperty(name = "otboo.storage.type", havingValue = "s3")
@Component
public class S3ImageStorage {
    private final S3Client s3Client;

    @Value("${otboo.storage.s3.bucket}")
    private String bucketName;

    @Value("${otboo.storage.s3.region}")
    private String region;

    public S3ImageStorage(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // 이미지 업로드
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

            log.info("[S3ImageStorage] 이미지 업로드 성공 - 경로: {}", s3Key);

            // 업로드된 객체의 공개 URL 반환
            return generatePublicUrl(s3Key);

        } catch (S3Exception | SdkClientException e) {
            log.error("[S3ImageStorage] S3 업로드 실패 - 경로: {}, 오류: {}", s3Key, e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "S3 업로드 중 외부 서비스 오류가 발생했습니다."
            );
        } catch (IOException e) {
            log.error("[S3ImageStorage] 이미지 업로드 실패 - 파일명: {}, 오류: {}", originalFileName, e.getMessage(), e);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일" + fileName + ") 입니다.");
        }
    }

    // 퍼블릭 URL 생성
    private String generatePublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }
}
